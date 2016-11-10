package org.tmoerman.plongeur.tda

import java.lang.Math.{PI, exp, min, sqrt}

import breeze.linalg.{SparseVector, max => elmax}
import org.apache.spark.mllib.feature.{PCA, PCAModel}
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.apache.spark.mllib.linalg.{Vector => MLLibVector}
import org.tmoerman.plongeur.tda.Distances.DistanceFunction
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.MapFunctions._
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
    * @param p The exponent
    * @param ctx
    * @param distance
    * @return Returns a Map by Index to the L_n eccentricity of that point.
    *         L_n eccentricity assigns to each point the distance to the point most distant from it.
    *
    *         See: Extracting insights from the shape of complex data using topology
    *              -- P. Y. Lum, G. Singh, [...], and G. Carlsson
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  @deprecated("use eccentricityVec instead")
  def eccentricityMap(p: Either[Int, _], ctx: ContextLike, distance: DistanceFunction): Map[Index, Double] = {
    val N = ctx.N

    val EMPTY: Map[Index, Double] = Map.empty

    p match {
      case Right(INFINITY) =>
        ctx
          .dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(max)(acc) },
            { case (acc1, acc2) => acc1.merge(max)(acc2) })

      case Left(1) =>
        ctx
          .dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, sum / N) }

      case Left(n) =>
        ctx
          .dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    val v = pow(d, n)
                                    Map(a.index -> v, b.index -> v).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, pow(sum, 1d / n) / N) }

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$p'")
    }}

  /**
    * @param p cfr. $L_p$ - the exponent of the norm
    * @param ctx
    * @param distance
    * @return Returns a MLLibVector with L_n eccentricity values of that point.
    *
    *         See: Extracting insights from the shape of complex data using topology
    *              -- P. Y. Lum, G. Singh, [...], and G. Carlsson
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def eccentricityVec(p: Either[Int, _], ctx: ContextLike, distance: DistanceFunction): MLLibVector = {
    val N = ctx.N

    val ZEROS: SparseVector[Double] = SparseVector.zeros(N)

    p match {
      case Right(INFINITY) =>
        val result: SparseVector[Double] =
          ctx
            .dataPoints
            .distinctComboPairs
            .aggregate(ZEROS)(
              { case (acc, (a, b)) => val d = distance(a, b)
                                      elmax(acc, SparseVector(N)(a.index -> d, b.index -> d)) },
              { case (acc1, acc2) => elmax(acc1, acc2) })

        result.toDenseVector.toMLLib

      case Left(1) =>
        val sums: SparseVector[Double] =
          ctx
            .dataPoints
            .distinctComboPairs
            .aggregate(ZEROS)(
              { case (acc, (a, b)) => val d = distance(a, b)
                                      acc += SparseVector(N)(a.index -> d, b.index -> d) },
              { case (acc1, acc2) => acc1 += acc2 })

        val result: SparseVector[Double] = sums /= N.toDouble

        result.toDenseVector.toMLLib

      case Left(n) =>
        val sums: SparseVector[Double] =
          ctx
            .dataPoints
            .distinctComboPairs
            .aggregate(ZEROS)(
              { case (acc, (a, b)) => val d = distance(a, b)
                                      val v = pow(d, n)
                                      acc += SparseVector(N)(a.index -> v, b.index -> v) },
              { case (acc1, acc2) => acc1 += acc2 })

        val result = (sums :^= (1d / n)) /= N.toDouble

        result.toDenseVector.toMLLib

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$p'")

    }}

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

    val ZEROS: SparseVector[Double] = SparseVector.zeros(N)

    val denominator = -2 * sigma * sigma

    val sums =
      ctx
        .dataPoints
        .distinctComboPairs
        .aggregate(ZEROS)(
          { case (acc, (a, b)) => val d = distance(a, b)
                                  val v = exp( pow(d, 2) / denominator )
                                  acc += SparseVector(N)(a.index -> v, b.index -> v) },
          { case (acc1, acc2) => acc1 += acc2 })

    val result = sums /= (N * pow(sqrt(2 * PI * sigma), D))

    result.toDenseVector.toMLLib
  }

}