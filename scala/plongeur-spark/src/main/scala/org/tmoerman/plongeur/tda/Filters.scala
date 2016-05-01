package org.tmoerman.plongeur.tda

import breeze.linalg.{Vector => MLVector}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance.DistanceFunction
import org.tmoerman.plongeur.tda.Model._
import shapeless._

import org.tmoerman.plongeur.util.MapFunctions._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

  /**
    * @param spec
    * @param dataPoints
    * @return Returns a FilterFunction for the specified filter specification.
    *         Closes over TDAContext for references to SparkContext and DataPoints.
    */
  def toFilterFunction(spec: HList, dataPoints: RDD[DataPoint])
                      (implicit sc: SparkContext): FilterFunction = spec match {

    case "feature" :: n :: HNil =>
      (d: DataPoint) => d.features(n.asInstanceOf[Int])

    case "centrality" :: n :: distanceSpec =>
      val broadcastFnMemo = centralityMap(dataPoints, n, toDistanceFunction(distanceSpec))

      (d: DataPoint) => sc.broadcast(broadcastFnMemo).value.apply(d.index)

    case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
  }

  def toDistanceFunction(distanceSpec: HList): DistanceFunction = distanceSpec match {
    case (name: String) :: HNil               => Distance.from(name)(Nil)
    case (name: String) :: (arg: Any) :: HNil => Distance.from(name)(arg)
    case _                                    => Distance.euclidean
  }

  /**
    * @param dataPoints
    * @param n The exponent
    * @param distance
    * @return Returns a Map by Index to the L_n centrality of that point.
    *         L_n centrality assigns to each point the distance to the point most distant from it.
    *
    *         See: Extracting insights from the shape of complex data using topology
    *              -- P. Y. Lum, G. Singh, [...], and G. Carlsson
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def centralityMap(dataPoints: RDD[DataPoint], n: Any, distance: DistanceFunction): Map[Index, Double] = {
    val EMPTY = Map[Index, Double]()

    val N = dataPoints.take(1).length

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
          .mapValues(sum => pow(sum, 1. / n) / N)

      case _ => throw new IllegalArgumentException(s"invalid value for centrality argument: '$n'")
    }}

}
