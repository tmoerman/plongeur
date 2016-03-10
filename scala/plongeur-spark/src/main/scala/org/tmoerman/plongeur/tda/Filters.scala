package org.tmoerman.plongeur.tda

import breeze.linalg.{Vector => MLVector}
import org.tmoerman.plongeur.tda.Model._
import shapeless._

/**
  * @author Thomas Moerman
  */
object Filters extends Serializable {

  def materialize(spec: HList, tdaContext: TDAContext): FilterFunction =
    spec match {
      case "feature" :: n :: HNil =>
        (d: DataPoint) => d.features(n.asInstanceOf[Int])

      case "centrality" :: (distance: String) :: HNil =>
        distanceToSampleMean(tdaContext, distance)

      case "centrality" :: (distance: String) :: (arg: String) :: HNil =>
        distanceToSampleMean(tdaContext, distance, arg.toInt)

      case _ => throw new IllegalArgumentException(s"could not materialize spec: $spec")
    }


  def eccentricity(tDAContext: TDAContext): FilterFunction = {



    ???
  }


  @Deprecated // TODO this is wrong - not equal to eccentricity or centrality as per the Ayasdi definition
  def distanceToSampleMean(tdaContext: TDAContext, name: String, arg: Any = AnyRef)
                          (d: DataPoint) = {
    import tdaContext._

    val center = (-1, sampleMean)
    val distanceFunction = Distance.from(name)(arg)

    distanceFunction(center, d)
  }

}
