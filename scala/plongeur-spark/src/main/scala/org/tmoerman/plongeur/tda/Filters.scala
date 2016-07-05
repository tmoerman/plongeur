package org.tmoerman.plongeur.tda

import breeze.linalg.{Vector => MLVector}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.feature.PCA
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, parseDistance}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.MapFunctions._
import shapeless.HList.ListCompat._
import shapeless._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

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

      case "PCA" #: n #: HNil =>
        val pcaMemo = ctx.memo(key).asInstanceOf[PCA]

        ???

      case "SVD" #: n #: HNil => ???

      case "eccentricity" #: n #: distanceSpec =>
        val eccMemo = ctx.memo(key).asInstanceOf[Map[Index, Double]]

        makeFn(ctx.sc.broadcast(eccMemo))

      case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
    }
  }

  val MAX_PCs: Int = 5

  def toFilterMemo(spec: HList, ctx: TDAContext): Option[(Any, Any)] =  {
    lazy val key = toFilterMemoKey(spec)

    spec match {
      case "PCA"          #: _ #: HNil         => Some(key -> new PCA(MAX_PCs).fit(ctx.dataPoints.map(_.features)))

      case "eccentricity" #: n #: distanceSpec => Some(key -> eccentricityMap(n, ctx, parseDistance(distanceSpec)))

      case _                                   => None
    }
  }

  def toFilterMemoKey(spec: HList) = spec match {
    case "PCA"          #: _ #: HNil => "PCA"
    case "eccentricity" #: n #: _    => s"ECC_$n"
    case _                           => throw new IllegalArgumentException(s"no memo key for filter spec: $spec")
  }

  def makeFn(bc: Broadcast[Map[Index, Double]]) = (d: DataPoint) => bc.value.apply(d.index)

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
          .mapValues(sum => sum / N)

      case n: Int =>
        combinations
          .aggregate(EMPTY)(
            { case (acc, (a, b)) => val d = distance(a, b)
                                    val v = pow(d, n)
                                    Map(a.index -> v, b.index -> v).merge(_ + _)(acc) },
            { case (acc1, acc2) => acc1.merge(_ + _)(acc2) })
          .mapValues(sum => pow(sum, 1d / n) / N)

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$n'")
    }}

}
