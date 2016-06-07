package org.tmoerman.plongeur.tda

import breeze.linalg.{Vector => MLVector}
import org.apache.spark.broadcast.Broadcast
import org.tmoerman.plongeur.tda.Distance.{EuclideanDistance, DistanceFunction}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.MapFunctions._
import shapeless._

import scala.math.{max, pow}

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

  /**
    * @param spec
    * @param tdaContext
    * @return Returns a FilterFunction for the specified filter specification.
    *         Closes over TDAContext for references to SparkContext and DataPoints.
    */
  def toFilterFunction(spec: HList, tdaContext: TDAContext): FilterFunction = spec match {

    case ("feature" | "features") :: (n: Int) :: HNil => (d: DataPoint) => d.features(n)

    case ("PCA" | "PC") :: n :: HNil =>
//      (d: DataPoint) => tdaContext.pca.transform(d.features).
//      val projected = data.map(p => p.copy(features = pca.transform(p.features)))
//      (d: DataPoint) => new LabeledPoint(d.features).copy()

      ???

    case ("SVD" | "SV") :: n :: HNil => ???

    case "eccentricity" :: n :: distanceSpec =>
      val broadcastFnMemo = eccentricityMap(n, tdaContext, toDistanceFunction(distanceSpec))

      makeFn(tdaContext.sc.broadcast(broadcastFnMemo))

    case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
  }

  def makeFn(bc: Broadcast[Map[Index, Double]]) = (d: DataPoint) => bc.value.apply(d.index)

  def toDistanceFunction(distanceSpec: HList): DistanceFunction = distanceSpec match {
    case (name: String) :: HNil               => Distance.from(name)(Nil)
    case (name: String) :: (arg: Any) :: HNil => Distance.from(name)(arg)
    case _                                    => EuclideanDistance
  }

  /**
    * @param n The exponent
    * @param tdaContext
    * @param distance
    * @return Returns a Map by Index to the L_n eccentricity of that point.
    *         L_n eccentricity assigns to each point the distance to the point most distant from it.
    *
    *         See: Extracting insights from the shape of complex data using topology
    *              -- P. Y. Lum, G. Singh, [...], and G. Carlsson
    *
    *         See: http://danifold.net/mapper/filters.html
    */
  def eccentricityMap(n: Any, tdaContext: TDAContext, distance: DistanceFunction): Map[Index, Double] = {
    import tdaContext._

    val EMPTY = Map[Index, Double]()

    val combinations =
      dataPoints
        .cartesian(dataPoints)                           // cartesian product
        .filter { case (p1, p2) => p1.index < p2.index } // combinations only

    n match {
      case "infinity" | "infty" | "_8"  =>
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

      case _ => throw new IllegalArgumentException(s"invalid value for eccentricity argument: '$n'")
    }}

}
