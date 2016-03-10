package org.tmoerman.plongeur.tda

import java.util.UUID

import org.apache.spark.mllib.linalg.{Vector => MLVector}

/**
  * @author Thomas Moerman
  */
object Model {

  def feature(n: Int) = (p: DataPoint) => p.features(n)

  implicit def pimp(in: (Int, MLVector)): DataPoint = dp(in._1, in._2)
  def dp(in: (Long, MLVector)): DataPoint = IndexedDataPoint(in._1, in._2)

  trait DataPoint {
    def features: MLVector
    def index: Long
  }

  case class IndexedDataPoint(val index: Long, val features: MLVector) extends DataPoint with Serializable

  type LevelSetID = Vector[BigDecimal]

  type LevelSetInverseFunction = (DataPoint) => Set[LevelSetID]

  type ID = UUID

  /**
    * @param id The cluster ID.
    * @param levelSetID The level set ID.
    * @param dataPoints The data points contained by this cluster.
    */
  case class Cluster(val id: ID,
                     val levelSetID: LevelSetID,
                     val dataPoints: Set[DataPoint]) extends Serializable {

    def size = dataPoints.size

    def verbose = s"""Cluster($id, $dataPoints)"""

    override def toString = s"Cluster($id)"

  }

  case class TDALens(val filters: Filter*) extends Serializable {

    // TODO the lens should accept a filter definition, not yet the filter function as this is computed in function of the data RDD.

    def functions =  filters.toArray.map(_.function)

  }

  type Boundaries = Array[(Double, Double)]

  type FilterFunction = (DataPoint) => Double

  type Percentage = BigDecimal

  case class Filter(val function: FilterFunction,
                    val length:   Percentage,
                    val overlap:  Percentage) extends Serializable {

    require(length >= 0 && length <= 1, "length must be a percentage.")
    require(overlap >= 0,               "overlap cannot be negative")
    require(overlap >= 2/3,             "overlap > 2/3 is discouraged")
  }

}