package org.tmoerman.plongeur.tda

import java.lang.Math.{PI, exp, min, sqrt}

import org.apache.spark.mllib.linalg.VectorConversions._
import breeze.linalg.{SparseVector, max => elmax}
import org.apache.spark.Logging
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.feature.{PCA, PCAModel}
import org.apache.spark.mllib.linalg.{Vector => MLLibVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, parseDistance}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.MapFunctions._
import org.tmoerman.plongeur.util.RDDFunctions._
import shapeless.HList.ListCompat._
import shapeless._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable with Logging {

  /**
    * @param filter
    * @param ctx
    * @return Returns a FilterFunction for the specified filter specification.
    *         Closes over TDAContext for references to SparkContext and DataPoints.
    */
  def toFilterFunction(filter: Filter, ctx: TDAContext): FilterFunction = {
    import filter._

    spec match {
      case "feature" #: (n: Int) #: HNil => (d: DataPoint) => d.features(n)

      case "PCA" #: (n: Int) #: HNil =>
        toBroadcastKey(filter)
          .flatMap(key => ctx.broadcasts.get(key))
          .map(bc => {
            val pcaModel = bc.value.asInstanceOf[PCAModel]

            (d: DataPoint) => pcaModel.transform(d.features)(n) })
          .get

      case _ => // broadcast value is a FilterFunction
        toBroadcastKey(filter)
          .flatMap(key => ctx.broadcasts.get(key))
          .map(bc => bc.value.asInstanceOf[FilterFunction]) // TODO incorrect ->
          .getOrElse(
            throw new IllegalArgumentException(s"no filter function for $spec, current broadcasts: " + ctx.broadcasts.keys.mkString(", ")))
    }
  }

  val MAX_PCs: Int = 10

  def toContextAmendment(filter: Filter) = toSketchAmendment(filter) andThen toBroadcastAmendment(filter)

  def toSketchAmendment(filter: Filter): ContextAmendment = (ctx: TDAContext) => {
    val amended = for {
      sketchKey <- toSketchKey(filter);

      ctx1 <- ctx.sketches
                 .get(sketchKey)
                 .orElse(filter.sketchParams.map(params => Sketch(ctx, params)))
                 .map(sketch => ctx.copy(sketches = ctx.sketches + (sketchKey -> sketch)));

      ctx2 <- ctx1.broadcasts
                  .get(sketchKey)
                  .orElse(ctx1.sketches.get(sketchKey).map(sketch => ctx1.sc.broadcast(sketch.toBroadcastValue)))
                  .map(value => ctx1.copy(broadcasts = ctx1.broadcasts + (sketchKey -> value)))

    } yield ctx2

    amended.getOrElse(ctx)
  }


  def toBroadcastAmendment(filter: Filter): ContextAmendment = (ctx: TDAContext) => {
    val amended = for {
      broadcastKey <- toBroadcastKey(filter);

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
      case "PCA" #: _ => {
        val pcaModel = new PCA(min(MAX_PCs, ctxLike.D)).fit(ctxLike.dataPoints.map(_.features))

        Some(pcaModel)
      }

      case "eccentricity" #: n #: distanceSpec => {
        val vec = eccentricityVec(n, ctxLike, parseDistance(distanceSpec))

        val fn = (d: DataPoint) => vec(d.index)

        Some(fn)
      }

      case "density" #: (sigma: Double) #: distanceSpec => {
        val vec = densityVec(sigma, ctxLike, parseDistance(distanceSpec))

        val fn = (d: DataPoint) => vec(d.index)

        Some(fn)
      }

      case _ => None
    }
  }

  def toSketchKey(filter: Filter): Option[SketchKey] = filter.sketchParams.map(_.toString)

  def toFilterSpecKey(filter: Filter): Option[BroadcastKey] =
    filter.spec match {
      case "PCA"          #: _                 => Some(s"PCA")
      case "eccentricity" #: n #: distanceSpec => Some(s"Eccentricity(n=$n, distance=${parseDistance(distanceSpec)})")
      case "density"      #: s #: distanceSpec => Some(s"Density(sigma=$s, distance=${parseDistance(distanceSpec)})")
      case _                                   => None
    }

  def toBroadcastKey(filter: Filter): Option[BroadcastKey] =
    toFilterSpecKey(filter).map(broadcastKey =>
      toSketchKey(filter).map(sketchKey => s"$broadcastKey, $sketchKey").getOrElse(broadcastKey))

  val EMPTY = Map[Index, Double]()

  /**
    * @param n The exponent
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
  def eccentricityMap(n: Any, ctx: ContextLike, distance: DistanceFunction): Map[Index, Double] = {
    val N = ctx.N

    n match {
      case "infinity" =>
        ctx
          .dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(max)(acc) },
            { case (acc1, acc2) => acc1.merge(max)(acc2) })

      case 1 =>
        ctx
          .dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, sum / N) }

      case n: Int =>
        ctx
          .dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    val v = pow(d, n)
                                    Map(a.index -> v, b.index -> v).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, pow(sum, 1d / n) / N) }

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$n'")
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
  def eccentricityVec(p: Any, ctx: ContextLike, distance: DistanceFunction): MLLibVector = {
    val N = ctx.N

    val ZEROS: SparseVector[Double] = SparseVector.zeros(N)

    p match {
      case "infinity" =>
        val result: SparseVector[Double] =
          ctx
            .dataPoints
            .distinctComboPairs
            .aggregate(ZEROS)(
              { case (acc, (a, b)) => val d = distance(a, b)
                                      elmax(acc, SparseVector(N)(a.index -> d, b.index -> d)) },
              { case (acc1, acc2) => elmax(acc1, acc2) })

        result.toDenseVector.toMLLib

      case 1 =>
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

      case n: Int =>
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

    val denom = -2 * sigma * sigma

    val sums =
      ctx
        .dataPoints
        .distinctComboPairs
        .aggregate(ZEROS)(
          { case (acc, (a, b)) => val d = distance(a, b)
                                  val v = exp( pow(d, 2) / denom )
                                  acc += SparseVector(N)(a.index -> v, b.index -> v) },
          { case (acc1, acc2) => acc1 += acc2 })

    val result = sums /= (N * pow(sqrt(2 * PI * sigma), D))

    result.toDenseVector.toMLLib
  }

}