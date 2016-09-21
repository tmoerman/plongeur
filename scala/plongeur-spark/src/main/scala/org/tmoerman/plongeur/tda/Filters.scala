package org.tmoerman.plongeur.tda

import java.lang.Math.{exp, sqrt, PI, min}

import breeze.linalg.{Vector => MLVector}
import org.apache.spark.Logging
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.feature.{PCA, PCAModel}
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, parseDistance}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.MapFunctions._
import shapeless.HList.ListCompat._
import shapeless._
import org.tmoerman.plongeur.util.RDDFunctions._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable with Logging {

  /**
    *
    *
    * @param spec
    * @param ctx
    * @return Returns a FilterFunction for the specified filter specification.
    *         Closes over TDAContext for references to SparkContext and DataPoints.
    */
  def toFilterFunction(spec: HList, ctx: TDAContext): FilterFunction = spec match {
      case "feature" #: (n: Int) #: HNil => (d: DataPoint) => d.features(n)

      case "PCA" #: (n: Int) #: HNil =>
        toBroadcastKey(spec)
            .flatMap(key => ctx.broadcasts.get(key))
            .map(bc => {
              val pcaModel = bc.value.asInstanceOf[PCAModel]
              (d: DataPoint) => pcaModel.transform(d.features)(n)
            })
            .get

      case _ => // broadcast value is a FilterFunction
        toBroadcastKey(spec)
          .flatMap(key => ctx.broadcasts.get(key))
          .map(bc => bc.value.asInstanceOf[FilterFunction]) // TODO incorrect ->
          .getOrElse(throw new IllegalArgumentException(
            s"no filter function for $spec, current broadcasts: " + ctx.broadcasts.keys.mkString(", ")))
    }

  val MAX_PCs: Int = 10

  def toBroadcastAmendment(spec: HList, ctx: TDAContext): Option[(String, () => Broadcast[Any])] =
    toBroadcastKey(spec).map(key => (key, () => {
      val v: Any = toBroadcastValue(spec, ctx)

      ctx.sc.broadcast(v)
    }))

  def toBroadcastValue(spec: HList, ctx: TDAContext): Any = spec match {
    case "PCA" #: (_: Int) #: HNil => {
      val pcaModel = new PCA(min(MAX_PCs, ctx.dim)).fit(ctx.dataPoints.map(_.features))

      pcaModel
    }

    case "eccentricity" #: n #: distanceSpec => {
      val map = eccentricityMap(n, ctx, parseDistance(distanceSpec))

      (d: DataPoint) => map.apply(d.index)
    }

    case _ => throw new IllegalArgumentException(s"No broadcast value for $spec")
  }

  def toBroadcastKey(spec: HList): Option[String] = spec match {
    case "PCA"          #: _ #: HNil => Some("PCA")
    case "eccentricity" #: n #: _    => Some(s"ECC[$n]")
    case _                           => None
  }

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
  def eccentricityMap(n: Any, ctx: TDAContext, distance: DistanceFunction): Map[Index, Double] = {
    import ctx._

    n match {
      case "infinity" =>
        dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(max)(acc) },
            { case (acc1, acc2) => acc1.merge(max)(acc2) })

      case 1 =>
        dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, sum / N) }

      case n: Int =>
        dataPoints
          .distinctComboPairs
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    val v = pow(d, n)
                                    Map(a.index -> v, b.index -> v).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, pow(sum, 1d / n) / N) }

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$n'")
    }}

  // TODO use sparse Vectors instead of maps!

  def densityMap(sigma: Double, ctx: TDAContext, distance: DistanceFunction): Map[Index, Double] = {
    import ctx._

    val denom = -2 * sigma * sigma

    dataPoints
      .distinctComboPairs
      .aggregate(EMPTY)(
        { case (acc, (a, b)) => val d = distance(a, b)
                                val v = exp( d*d / denom )
                                Map(a.index -> v, b.index -> v).merge(_ + _)(acc) },
        { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
      .map{ case (i, sum) => (i, sum / N * pow(sqrt(2 * PI * sigma), dim))}
  }

}