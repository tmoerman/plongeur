package org.tmoerman.plongeur.tda

import breeze.linalg.{Vector => MLVector}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance.DistanceFunction
import org.tmoerman.plongeur.tda.Model._
import shapeless._

import org.tmoerman.plongeur.util.MapFunctions._

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
  def reify(spec: HList, dataPoints: RDD[DataPoint])
           (implicit sc: SparkContext): FilterFunction = spec match {

    case "feature" :: n :: HNil =>
      (d: DataPoint) => d.features(n.asInstanceOf[Int])
      
    case "centrality" :: "L_infinity" :: distanceSpec =>
      val distanceFunction = distanceSpec match {
        case (name: String) :: HNil               => Distance.from(name)(Nil)
        case (name: String) :: (arg: Any) :: HNil => Distance.from(name)(arg)
        case _                                    => Distance.euclidean
      }

      val f = L_InfinityCentralityMap(dataPoints, distanceFunction)

      val bc = sc.broadcast(f)

      (d: DataPoint) => bc.value.apply(d.index)

    case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
  }

  /**
    * @param dataPoints An RDD of DataPoint instances
    * @param distanceFunction A DistanceFunction
    * @return Returns a Map by Index to the L-infinity centrality of that point.
    *         L-infinity centrality assigns to each point the distance to the point most distant from it.
    *
    *         See: 'Extracting insights from the shape of complex data using topology'
    *               P. Y. Lum, G. Singh, [...], and G. Carlsson
    */
  def L_InfinityCentralityMap(dataPoints: RDD[DataPoint], distanceFunction: DistanceFunction): Map[Index, Double] = {
    val max: (Double, Double) => Double = math.max

    dataPoints
      .cartesian(dataPoints)                          // cartesian product
      .filter{ case (p1, p2) => p1.index < p2.index } // combinations only
      .aggregate(Map[Index, Double]())(
        seqOp = { case (acc, (a, b)) => val v = distanceFunction(a, b)
                                       Map(a.index -> v, b.index -> v).merge(max)(acc) },
        combOp = { case (acc1, acc2) => acc1.merge(max)(acc2) })
  }

}
