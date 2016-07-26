package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.UUID

import com.softwaremill.quicklens
import com.softwaremill.quicklens._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.IndexedRow
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.cluster.Clustering.{ScaleSelection, ClusteringParams}
import org.tmoerman.plongeur.tda.cluster.Scale._
import shapeless.contrib.scalaz._
import shapeless.{HList, lens}

import scala.util.Try
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

    def asIndexedRow = IndexedRow(index, features)
  }

  case class IndexedDataPoint(val index: Long,
                              val features: MLVector,
                              val meta: Option[Map[String, _ <: Serializable]] = None) extends DataPoint with Serializable

  type LevelSetID = Vector[BigDecimal]

  type LevelSetsInverseFunction = (DataPoint) => Set[LevelSetID]

  type ID = UUID

  /**
    * @param id         The cluster ID.
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

  @deprecated("disappears in the light of the meta machine!")
  case class TDAContext(val sc: SparkContext,
                        val dataPoints: RDD[DataPoint],
                        val memo: Map[String, Any] = Map()) extends Serializable {

    lazy val N = dataPoints.count

    lazy val dim = dataPoints.first.features.size

    def updateMemo(f: Map[String, Any] => Map[String, Any]) = copy(memo = f(memo))

  }

  case class TDAParams(val lens: TDALens,
                       val clusteringParams: ClusteringParams,
                       val collapseDuplicateClusters: Boolean = true,
                       val scaleSelection: ScaleSelection = histogram(),
                       val coveringBoundaries: Option[Boundaries] = None) extends Serializable //TODO remove boundaries -> not useful

  object TDAParams {

    import quicklens._

    val modFilterNrBins = modify(_: Filter)(_.nrBins)

    val modFilterOverlap = modify(_: Filter)(_.overlap)

    def modFilter(i: Int) = modify(_: TDAParams)(_.lens.filters.at(i))

    def setFilterNrBins(i: Int, value: Int) =
      (params: TDAParams) => Try((modFilter(i) andThenModify modFilterNrBins) (params).setTo(value)).getOrElse(params)

    def setFilterOverlap(i: Int, value: Percentage) =
      (params: TDAParams) => Try((modFilter(i) andThenModify modFilterOverlap) (params).setTo(value)).getOrElse(params)

    def setHistogramScaleSelectionNrBins(nrBins: Int) =
      (params: TDAParams) => modify(params)(_.scaleSelection).setTo(HistogramScaleSelection(nrBins))

  }

  case class TDAResult(val clustersRDD: RDD[Cluster],
                       val edgesRDD: RDD[Set[ID]]) extends Serializable {

    lazy val clusters = clustersRDD.collect

    lazy val edges = edgesRDD.collect

  }

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

    def apply(filters: Filter*): TDALens = TDALens(filters.toList)

  }

  case class Filter(val spec: HList,
                    val nrBins: Int,
                    val overlap: Percentage,
                    val balanced: Boolean = false) extends Serializable {

    require(nrBins > 0, "nrBins must be greater than 0")
    require(overlap >= 0, "overlap cannot be negative")
  }

}