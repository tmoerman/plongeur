package org.tmoerman.plongeur.tda

import java.util.UUID
import java.util.UUID.randomUUID

import org.apache.spark.mllib.regression.LabeledPoint

/**
  * Model types for the TDA mapper.
  *
  * @author Thomas Moerman
  */
object Model extends Serializable {

  // TDA

  type HyperCubeCoordinateVector = Vector[BigDecimal]

  type CoveringFunction = (LabeledPoint) => Set[HyperCubeCoordinateVector]

  case class Lens(val filters: Filter*) extends Serializable {

    def functions =  filters.toArray.map(_.function)

  }

  type FilterFunction = (LabeledPoint) => Double

  def feature(n: Int) = (p: LabeledPoint) => p.features(n)

  type Percentage = BigDecimal

  case class Filter(val function: FilterFunction,
                    val length:   Percentage,
                    val overlap:  Percentage) extends Serializable {

    require(length >= 0 && length <= 1, "length must be a percentage.")
    require(overlap >= 0,               "overlap cannot be negative")
    require(overlap >= 2/3,             "overlap > 2/3 is discouraged")
  }

}