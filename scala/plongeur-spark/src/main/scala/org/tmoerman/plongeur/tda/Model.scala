package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.regression.LabeledPoint

/**
  * Model types for the TDA mapper.
  *
  * @author Thomas Moerman
  */
object Model extends Serializable {

  type HyperCubeCoordinates = Vector[Any]

  type CoveringFunction = (LabeledPoint) => Set[HyperCubeCoordinates]

  type DistanceFunction = (LabeledPoint, LabeledPoint) => Double

  val pearsonDistance: DistanceFunction = ???

  val spearmanDistance: DistanceFunction = ???

  case class Lens(val filters: Array[Filter]) extends Serializable {

    def functions =  filters.map(_.function)

  }

  type FilterFunction = (LabeledPoint) => Double

  type Percentage = BigDecimal

  case class Filter(val function: FilterFunction,
                    val length:   Percentage,
                    val overlap:  Percentage) extends Serializable {

    require(length >= 0 && length <= 1, "length must be a percentage.")

    require(overlap >= 0,   "overlap cannot be negative")
    require(overlap <= 2/3, "overlap > 2/3 is discouraged")
  }

  /*



   */

}