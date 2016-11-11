package org.tmoerman.plongeur.tda

import java.lang.Math.{PI, exp, min, sqrt}

import breeze.linalg.{max => elmax}
import org.apache.spark.mllib.feature.{PCA, PCAModel}
import org.apache.spark.mllib.linalg.{DenseVector => MLLibDenseVector, Vector => MLLibVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances.{Distance, DistanceFunction}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.RDDFunctions._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

  /**
    * @param filter
    * @param ctx
    * @return Returns a FilterFunction for the specified filter specification.
    *         Closes over TDAContext for references to SparkContext and DataPoints.
    */
  def toFilterFunction(filter: Filter, ctx: TDAContext): FilterFunction = {
    import filter._

    val broadcasts = ctx.broadcasts

    val result = (spec, sketch) match {

      case (Feature(n), _) =>
        val fn = (d: DataPoint) => d.features(n)
        Some(fn)

      case (PrincipalComponent(n), _) => for {
        broadcastKey      <- toBroadcastKey(filter)
        broadcastPCAModel <- broadcasts.get(broadcastKey).map(_.value.asInstanceOf[PCAModel]) }

        yield (d: DataPoint) => broadcastPCAModel.transform(d.features)(n)

      case (_: Eccentricity | _: Density, None) => for {
        broadcastKey    <- toBroadcastKey(filter)
        broadcastVector <- broadcasts.get(broadcastKey).map(_.value.asInstanceOf[MLLibVector]) }

        yield (d: DataPoint) => broadcastVector(d.index)

      case (_: Eccentricity | _: Density, Some(params)) => for {
        broadcastKey    <- toBroadcastKey(filter)
        broadcastVector <- broadcasts.get(broadcastKey).map(_.value.asInstanceOf[MLLibVector])
        sketchKey       <- toSketchKey(filter)
        broadcastLookup <- broadcasts.get(sketchKey).map(_.value.asInstanceOf[Map[Index, Index]])}

        yield (d: DataPoint) => broadcastVector(broadcastLookup(d.index))

      case _ => None
    }

    result.getOrElse(throw new IllegalArgumentException(
      s"No filter function for ($spec, $sketch). Current broadcasts: ${broadcasts.keys.mkString(", ")}."))
  }

  val MAX_PCs: Int = 10

  def toContextAmendment(filter: Filter) = toSketchAmendment(filter) andThen toBroadcastAmendment(filter)

  /**
    * @param filter
    * @return Returns a ContextAmendment function that updates both the specified TDAContext's sketches and broadcasts
    *         in function of the
    */
  def toSketchAmendment(filter: Filter): ContextAmendment = (ctx: TDAContext) => {
    val amended = for {
      sketchKey <- toSketchKey(filter);

      ctx1 <- ctx.sketches
                 .get(sketchKey)
                 .orElse(filter.sketch.map(params => Sketch(ctx, params)))
                 .map(sketch => ctx.copy(sketches = ctx.sketches + (sketchKey -> sketch)));

      ctx2 <- ctx1.broadcasts
                  .get(sketchKey)
                  .orElse(ctx1.sketches.get(sketchKey).map(sketch => ctx1.sc.broadcast(sketch.lookupMap)))
                  .map(value => ctx1.copy(broadcasts = ctx1.broadcasts + (sketchKey -> value)))

    } yield ctx2

    amended.getOrElse(ctx)
  }

  /**
    * @param filter
    * @return
    */
  def toBroadcastAmendment(filter: Filter): ContextAmendment = (ctx: TDAContext) => {
    val amended = for {
      broadcastKey <- toBroadcastKey(filter)
      ctx1 <- ctx.broadcasts
                 .get(broadcastKey)
                 .orElse(toBroadcastValue(filter, ctx).map(value => ctx.sc.broadcast(value)))
                 .map(value => ctx.copy(broadcasts = ctx.broadcasts + (broadcastKey -> value)))
    } yield ctx1

    amended.getOrElse(ctx)
  }

  def toBroadcastValue(filter: Filter, ctx: TDAContext): Option[_] = {
    import filter._

    val ctxLike: ContextLike = toSketchKey(filter).flatMap(key => ctx.sketches.get(key)).getOrElse(ctx)

    spec match {

      case PrincipalComponent(_) => {
        val pcaModel = new PCA(min(MAX_PCs, ctxLike.D)).fit(ctxLike.dataPoints.map(_.features))
        Some(pcaModel)
      }

      case LaplacianEigenVector(_) => {
        val laplacianModel = ???

        Some(laplacianModel)
      }

      case Eccentricity(n, distance) => {
        val vec = eccentricityVec(n, ctxLike, distance)
        Some(vec)
      }

      case Density(sigma, distance) => {
        val vec = densityVec(sigma, ctxLike, distance)
        Some(vec)
      }

      case _ => None
    }
  }

  def toSketchKey(filter: Filter): Option[SketchKey] = filter.sketch

  def toFilterSpecKey(spec: FilterSpec): Option[BroadcastKey] =
    spec match {
      case _: PrincipalComponent   |
           _: LaplacianEigenVector |
           _: Eccentricity         |
           _: Density => Some(spec)

      case _: Feature => None
    }

  def toBroadcastKey(filter: Filter): Option[BroadcastKey] =
    toFilterSpecKey(filter.spec)
      .map(broadcastKey =>
        toSketchKey(filter)
          .map(sketchKey => (broadcastKey, sketchKey))
          .getOrElse(broadcastKey))

  /**
    * @param p
    * @param ctx
    * @param distance
    * @return Returns an RDD by Index to the L_n eccentricity of that point.
    *         L_n eccentricity assigns to each point the distance to the point most distant from it.
    *
    *         See: Extracting insights from the shape of complex data using topology
    *              -- P. Y. Lum, G. Singh, [...], and G. Carlsson
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def eccentricityRDD(p: Either[Int, _], ctx: ContextLike, distance: DistanceFunction): RDD[(Index, Double)] = {
    val N = ctx.N

    p match {

      case Right(INFINITY) =>
        unfold(ctx.dataPoints, distance)
          .reduceByKey(max)

      case Left(1) =>
        unfold(ctx.dataPoints, distance)
          .reduceByKey(_ + _)
          .map{ case (i, sum) => (i, sum / N) }

      case Left(n) =>
        unfold(ctx.dataPoints, distance)
          .mapValues(d => pow(d, n))
          .reduceByKey(_ + _)
          .map{ case (i, sum) => (i, pow(sum, 1d / n) / N) }

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$p'")
    }
  }

  def eccentricityMap(p: Either[Int, _], ctx: ContextLike, distance: DistanceFunction): Map[Index, Double] =
    eccentricityRDD(p, ctx, distance).collectAsMap.toMap

  def eccentricityVec(p: Either[Index, _], ctx: ContextLike, distance: DistanceFunction): MLLibVector =
    toMLVector(eccentricityRDD(p, ctx, distance))

  /**
    * @param sigma
    * @param ctx
    * @param distance
    * @return TODO docs
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def densityVec(sigma: Double, ctx: ContextLike, distance: DistanceFunction): MLLibVector = {
    val N = ctx.N
    val D = ctx.D

    val denominator = -2 * sigma * sigma

    val sums =
      unfold(ctx.dataPoints, distance)
        .mapValues(d => exp(pow(d, 2) / denominator))
        .reduceByKey(_ + _)
        .mapValues(_ / (N * pow(sqrt(2 * PI * sigma), D)))

    toMLVector(sums)
  }

  def toMLVector(sums: RDD[(Index, Distance)]): MLLibVector =
    new MLLibDenseVector(sums.sortByKey().map(_._2).collect)

  def unfold(points: RDD[DataPoint], distance: DistanceFunction): RDD[(Index, Distance)] =
    points
      .distinctComboPairs
      .flatMap { case (p, q) => val d = distance(p, q); (p.index, d) ::(q.index, d) :: Nil }

}