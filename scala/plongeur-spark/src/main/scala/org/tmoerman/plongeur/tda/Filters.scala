package org.tmoerman.plongeur.tda

import breeze.linalg.{Vector => MLVector}
import org.tmoerman.plongeur.tda.Distance.DistanceFunction
import org.tmoerman.plongeur.tda.Model._
import shapeless._

import org.tmoerman.plongeur.util.MapFunctions._

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

  def materialize(spec: HList, tdaContext: TDAContext): FilterFunction =
    spec match {
      case "feature" :: n :: HNil =>
        (d: DataPoint) => d.features(n.asInstanceOf[Int])

      case "centrality" :: "L_infinity" :: distanceSpec =>

        val (distance: String, arg: Any) = distanceSpec match {
          case (name: String) :: HNil => (name, null)
          case (name: String) :: (arg: Any) :: HNil => (name, arg)
        }

        val distanceFunction = Distance.from(distance)(arg)

        val f = L_inf_Centrality(tdaContext, distanceFunction) // cache this

        (d: DataPoint) => f(d)

      case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
    }

  def L_inf_Centrality(tdaContext: TDAContext, distance: DistanceFunction): FilterFunction = {
    val min: (Double, Double) => Double = math.min

    val rdd = tdaContext.dataPoints

    val memo: Map[Index, Double] =
      rdd
        .cartesian(rdd)                                 // cartesian product
        .filter{ case (p1, p2) => p1.index < p2.index } // combinations only
        .aggregate(Map[Index, Double]())(
          {case (acc, (a, b)) =>
            val v = distance(a, b);
            Map(a.index -> v, b.index -> v).merge(min)(acc)},
          {case (acc1, acc2) =>
            acc1.merge(min)(acc2)})

    (d: DataPoint) => memo(d.index)
  }

}
