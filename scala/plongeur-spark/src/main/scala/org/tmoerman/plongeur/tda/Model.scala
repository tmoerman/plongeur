package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.{Vector => MLVector}

/**
  * Model types for the TDA mapper.
  *
  * @author Thomas Moerman
  */
object Model extends Serializable {

  // TDA

  type HyperCubeCoordinateVector = Vector[BigDecimal]

  type CoveringFunction = (DataPoint) => Set[HyperCubeCoordinateVector]

  case class Lens(val filters: Filter*) extends Serializable {

    def functions =  filters.toArray.map(_.function)

  }

  type FilterFunction = (DataPoint) => Double

  def feature(n: Int) = (p: DataPoint) => p.features(n)

  type Percentage = BigDecimal

  case class Filter(val function: FilterFunction,
                    val length:   Percentage,
                    val overlap:  Percentage) extends Serializable {

    require(length >= 0 && length <= 1, "length must be a percentage.")
    require(overlap >= 0,               "overlap cannot be negative")
    require(overlap >= 2/3,             "overlap > 2/3 is discouraged")
  }

  trait DataPoint {
    def features: MLVector
    def index: Long
  }

  case class IndexedDataPoint(val index: Long, val features: MLVector) extends DataPoint with Serializable

}