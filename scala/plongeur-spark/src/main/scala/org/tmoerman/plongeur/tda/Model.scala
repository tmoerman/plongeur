package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.regression.LabeledPoint

/**
  * Model types for the TDA mapper.
  *
  * @author Thomas Moerman
  */
object Model extends Serializable {

  type A_id = Vector[BigDecimal] // ID for 1 n-dimensional A bin

  type CoverageFunction = (LabeledPoint) => Seq[A_id]

  case class Lens(val filters: Array[Filter]) extends Serializable {

    def functions =  filters.map(_.function)

  }

  type FilterFunction = (LabeledPoint) => Double

  type Percentage = Double

  case class Filter(val function:    FilterFunction,
                    val length:   Percentage,
                    val overlap:  Percentage) extends Serializable {

    require(length >= 0.0 && length <= 1.0, "length must be a percentage.")

    require(overlap >= 0.0,   "overlap cannot be negative")
    require(overlap <= 0.666, "overlap > 2/3 is discouraged")
  }

}