package org.tmoerman.plongeur.tda

import java.lang.Math.min

import breeze.linalg.{Vector => MLVector}
import org.apache.spark.Logging
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.feature.{PCAModel, PCA}
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, parseDistance}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.MapFunctions._
import shapeless.HList.ListCompat._
import shapeless._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable with Logging {

  /**
    * @param spec
    * @param ctx
    * @return Returns a FilterFunction for the specified filter specification.
    *         Closes over TDAContext for references to SparkContext and DataPoints.
    */
  def toFilterFunction(spec: HList, ctx: TDAContext): FilterFunction = {
    lazy val key = toFilterMemoKey(spec)

    spec match {

      case "feature" #: (n: Int) #: HNil => (d: DataPoint) => d.features(n)

      case "PCA" #: (n: Int) #: HNil =>
        val pcaMemo = ctx.memo(key.get).asInstanceOf[PCAModel]

        val bc = ctx.sc.broadcast(pcaMemo)

        (d: DataPoint) => bc.value.transform(d.features)(n)


      case "SVD" #: (n: Int) #: HNil => ???

      case "eccentricity" #: n #: distanceSpec =>
        val eccMemo = ctx.memo(key.get).asInstanceOf[Map[Index, Double]]

        val bc = ctx.sc.broadcast(eccMemo)

        (d: DataPoint) => bc.value.apply(d.index)

      case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
    }
  }

  val MAX_PCs: Int = 10

  def toFilterMemo(spec: HList, ctx: TDAContext): Option[(String, Any)] = {
    toFilterMemoKey(spec).flatMap(key =>
      ctx
        .memo
        .get(key)
        .map(v => (key -> v))
        .orElse{
          spec match {
            case "PCA"          #: _ #: HNil         => Some(key -> new PCA(min(MAX_PCs, ctx.dim)).fit(ctx.dataPoints.map(_.features)))

            case "eccentricity" #: n #: distanceSpec => Some(key -> eccentricityMap(n, ctx, parseDistance(distanceSpec)))

            case _                                   => None
          }})
  }

  def toFilterMemoKey(spec: HList): Option[String] = spec match {
    case "PCA"          #: _ #: HNil => Some("PCA")
    case "eccentricity" #: n #: _    => Some(s"ECC_$n")
    case _                           => None
  }

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

    val EMPTY = Map[Index, Double]()

    val combinations =
      dataPoints
        .cartesian(dataPoints)                           // cartesian product
        .filter { case (p1, p2) => p1.index < p2.index } // combinations only

    n match {
      case "infinity" =>
        combinations
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(max)(acc) },
            { case (acc1, acc2) => acc1.merge(max)(acc2) })

      case 1 =>
        combinations
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    Map(a.index -> d, b.index -> d).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, sum / N) }

      case n: Int =>
        combinations
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    val v = pow(d, n)
                                    Map(a.index -> v, b.index -> v).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .map{ case (i, sum) => (i, pow(sum, 1d / n) / N) }

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$n'")
    }}

}
