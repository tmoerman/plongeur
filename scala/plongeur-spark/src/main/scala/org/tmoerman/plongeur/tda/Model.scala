package org.tmoerman.plongeur.tda

import java.util.UUID

import org.apache.spark.mllib.linalg.{Vector => MLVector}
import shapeless.HList

/**
  * @author Thomas Moerman
  */
object Model {

  def feature(n: Int) = (p: DataPoint) => p.features(n)

  implicit def pimp(in: (Int, MLVector)): DataPoint = dp(in._1, in._2)
  def dp(in: (Long, MLVector)): DataPoint = IndexedDataPoint(in._1, in._2)

  type Index = Long

  trait DataPoint {
    def features: MLVector
    def index: Index
  }

  case class IndexedDataPoint(val index: Long, val features: MLVector) extends DataPoint with Serializable

  type LevelSetID = Vector[BigDecimal]

  type LevelSetsInverseFunction = (DataPoint) => Set[LevelSetID]

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

  type Boundaries = Array[(Double, Double)]

  type FilterFunction = (DataPoint) => Double

  type Percentage = BigDecimal

  case class TDALens(val filters: Filter*) extends Serializable

  case class Filter(val spec:     HList,
                    val length:   Percentage,
                    val overlap:  Percentage,
                    val balanced: Boolean = false) extends Serializable {

    require(length >= 0 && length <= 1, "length must be a percentage.")
    require(overlap >= 0,               "overlap cannot be negative")
    require(overlap >= 2/3,             "overlap > 2/3 is discouraged")
  }

}