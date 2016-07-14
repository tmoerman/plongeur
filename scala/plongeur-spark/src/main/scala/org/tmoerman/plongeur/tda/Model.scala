package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.UUID

import org.apache.spark.mllib.linalg.{Vector => MLVector}
import shapeless.contrib.scalaz._
import shapeless.{HList, lens}

import scalaz.PLens._

/**
  * @author Thomas Moerman
  */
object Model {

  def feature(n: Int) = (p: DataPoint) => p.features(n)

  implicit def pimp(in: (Int, MLVector)): DataPoint = dp(in._1, in._2)
  def dp(in: (Long, MLVector)): DataPoint = IndexedDataPoint(in._1, in._2)

  type Index = Long

  trait DataPoint {
    def index: Index
    def features: MLVector
    def meta: Option[Map[String, _ <: Serializable]]
  }

  case class IndexedDataPoint(val index: Long,
                              val features: MLVector,
                              val meta: Option[Map[String, _ <: Serializable]] = None) extends DataPoint with Serializable

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

  case class TDALens(val filters: List[Filter]) extends Serializable {

    def assocFilterMemos(tdaContext: TDAContext): TDAContext =
      filters.foldLeft(tdaContext) {
        (ctx, filter) =>
          Filters
            .toFilterMemo(filter.spec, ctx)
            .map(pair => ctx.updateMemo(memo => memo + pair))
            .getOrElse(ctx)
      }

  }

  object TDALens {

    /**
      * Factory method with var arg filters.
 *
      * @param filters
      * @return Returns a TDALens
      */
    def apply(filters: Filter*): TDALens = TDALens(filters.toList)

  }

  case class Filter(val spec:     HList,
                    val nrBins:   Int,
                    val overlap:  Percentage,
                    val balanced: Boolean = false) extends Serializable {

    require(nrBins > 0,     "nrBins must be greater than 0")
    require(overlap >= 0,   "overlap cannot be negative")
  }

  /**
    * Hosts model lenses (cfr. Scalaz).
    */
  object L {
    val filtersLens = (lens[TDAParams] >> 'lens >> 'filters).asScalaz.partial

    def filter(i: Int) = filtersLens.andThen(listNthPLens(i))

    val nrBinsLens = (lens[Filter] >> 'nrBins).asScalaz.partial

    def nrBins(i: Int) = filter(i).andThen(nrBinsLens)

    def setNrBins(i: Int, nr: Int) = (p: TDAParams) => nrBins(i).set(p, nr).get
    
  }

}