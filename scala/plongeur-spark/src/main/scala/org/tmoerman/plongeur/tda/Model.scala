package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.UUID

import com.softwaremill.quicklens
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Filters.toBroadcastAmendment
import org.tmoerman.plongeur.tda.cluster.Clustering.{ClusteringParams, ScaleSelection}
import org.tmoerman.plongeur.tda.cluster.Scale._
import shapeless.HList

import scala.util.Try

/**
  * @author Thomas Moerman
  */
object Model {

  def feature(n: Int) = (p: DataPoint) => p.features(n)

  implicit def pimp(in: (Index, MLVector)): DataPoint = dp(in._1, in._2)

  def dp(index: Long, features: MLVector): DataPoint = IndexedDataPoint(index.toInt, features)

  type Index = Int

  trait DataPoint {
    def index: Index

    def features: MLVector

    def meta: Option[Map[String, _ <: Serializable]]
  }

  case class IndexedDataPoint(val index: Index,
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

  case class TDAContext(val sc: SparkContext,
                        val dataPoints: RDD[DataPoint],
                        val broadcasts: Map[String, Broadcast[Any]] = Map()) extends Serializable {

    val self = this

    lazy val N = dataPoints.count

    lazy val dim = dataPoints.first.features.size

    def addBroadcast(key: String, producer: () => Broadcast[Any]) =
      broadcasts
        .get(key)
        .map(_ => self)
        .getOrElse(self.copy(broadcasts = broadcasts + (key -> producer.apply())))
  }

  case class TDAParams(val lens: TDALens,
                       val clusteringParams: ClusteringParams = ClusteringParams(),
                       val collapseDuplicateClusters: Boolean = true,
                       val scaleSelection: ScaleSelection = histogram()) extends Serializable {

    def amend(ctx: TDAContext): TDAContext =
      lens
        .filters
        .map(f => toBroadcastAmendment(f.spec, ctx))
        .flatten
        .foldLeft(ctx){ case (c, (key, fn)) => c.addBroadcast(key, fn) }

  }

  object TDAParams {

    import quicklens._

    val modFilterNrBins = modify(_: Filter)(_.nrBins)

    val modFilterOverlap = modify(_: Filter)(_.overlap)

    def modFilter(idx: Int) = modify(_: TDAParams)(_.lens.filters.at(idx))

    def setFilterNrBins(idx: Int, value: Int) =
      (params: TDAParams) => Try((modFilter(idx) andThenModify modFilterNrBins) (params).setTo(value)).getOrElse(params)

    def setFilterOverlap(idx: Int, value: Percentage) =
      (params: TDAParams) => Try((modFilter(idx) andThenModify modFilterOverlap) (params).setTo(value)).getOrElse(params)

    def setHistogramScaleSelectionNrBins(nrBins: Int) =
      (params: TDAParams) => modify(params)(_.scaleSelection).setTo(HistogramScaleSelection(nrBins))

    def setCollapseDuplicateClusters(collapse: Boolean) =
      (params: TDAParams) => params.copy(collapseDuplicateClusters = collapse)

    def setDistanceFunction = (params: TDAParams) => params // TODO implement

  }

  case class TDAResult(val clustersRDD: RDD[Cluster],
                       val edgesRDD: RDD[Set[ID]]) extends Serializable {

    lazy val clusters = clustersRDD.collect

    lazy val edges = edgesRDD.collect

  }

  case class TDALens(val filters: List[Filter]) extends Serializable

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