package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.UUID

import com.softwaremill.quicklens
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Colour.{Nop, Colouring}
import org.tmoerman.plongeur.tda.Distances.{DEFAULT_DISTANCE, DistanceFunction}
import org.tmoerman.plongeur.tda.Filters.{FilterRDDFactory, toContextAmendment}
import org.tmoerman.plongeur.tda.Sketch.SketchParams
import org.tmoerman.plongeur.tda.cluster.Clustering.{ClusteringParams, ScaleSelection}
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.tda.geometry.Laplacian.DEFAULT_SIGMA
import org.tmoerman.plongeur.tda.knn.KNN_RDD

import scala.collection.immutable.Map.empty
import scala.util.Try

/**
  * TODO restructure the packaging hierarchy, e.g. tda and knn should be on equal level instead of nested...
  *
  * @author Thomas Moerman
  */
object Model {

  def feature(n: Int) = (p: DataPoint) => p.features(n)

  type Count = Int
  type Index = Int

  implicit def pimp(in: (Index, MLVector)): DataPoint = dp(in._1, in._2)

  def dp(index: Long,
         features: MLVector) = DataPoint(index.toInt, features)

  def dp(index: Long,
         features: MLVector,
         meta: Map[String, _ <: Serializable]) = DataPoint(index.toInt, features, Some(meta))

  case class DataPoint(val index: Index,
                       val features: MLVector,
                       val meta: Option[Map[String, _ <: Serializable]] = None) extends Serializable

  type LevelSetID = Vector[BigDecimal]

  type LevelSetsInverseFunction = (DataPoint) => Set[LevelSetID]

  type ID = UUID

  type ClusterEdge = Seq[ID]

  /**
    * @param id         The cluster ID.
    * @param levelSetID The level set ID.
    * @param dataPoints The data points contained by this cluster.
    * @param colour     The colour.
    */
  case class Cluster(val id: ID,
                     val levelSetID: LevelSetID,
                     val dataPoints: Set[DataPoint],
                     val colour: Option[String] = None) extends Serializable {

    def size = dataPoints.size

    def verbose = s"""Cluster($id, $dataPoints)"""

    override def toString = s"Cluster($id)"

    def colours = colour.toSeq

  }

  type Boundaries = Seq[(Double, Double)]

  type FilterFunction = (DataPoint) => Double

  type Percentage = BigDecimal

  type CacheKey = Serializable

  type ContextAmendment = TDAContext => TDAContext

  trait ContextLike {
    def N: Int
    def D: Int
    def dataPoints: RDD[DataPoint]
  }

  type Broadcasts  = Map[CacheKey, Broadcast[_]]
  type SketchCache = Map[CacheKey, Sketch]
  type FilterCache = Map[CacheKey, FilterRDDFactory]
  type KNNCache    = Map[CacheKey, KNN_RDD]

  /**
    * State data structure for the TDA pipeline.
    *
    * @param sc
    * @param dataPoints
    */
  case class TDAContext(sc: SparkContext,
                        dataPoints: RDD[DataPoint],
                        broadcasts:  Broadcasts  = empty,
                        filterCache: FilterCache = empty,
                        knnCache:    KNNCache    = empty,
                        sketchCache: SketchCache = empty) extends ContextLike with Serializable {

    val self = this

    lazy val N = dataPoints.count.toInt

    lazy val D = dataPoints.first.features.size

    lazy val indexBound = dataPoints.map(_.index).max + 1

    lazy val stats = Statistics.colStats(dataPoints.map(_.features))
  }

  case class TDAParams(val lens: TDALens,
                       val clusteringParams: ClusteringParams = ClusteringParams(),
                       val collapseDuplicateClusters: Boolean = true,
                       val scaleSelection: ScaleSelection = histogram(),
                       val colouring: Colouring = Nop()) extends Serializable {

    def amend(ctx: TDAContext): TDAContext =
      lens
        .filters
        .map(toContextAmendment)
        .foldLeft(ctx){ case (acc, amendment) => amendment.apply(acc) }

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
                       val edgesRDD: RDD[ClusterEdge]) extends Serializable {

    lazy val clusters = clustersRDD.collect

    lazy val edges = edgesRDD.collect

  }

  /**
    * @param filters
    * @param partitionByLevelSetID Activate RangePartitioning by levelSet ID, is probably more efficient than hash
    *                              partitioning on LevelSetIDs.
    */
  case class TDALens(val filters: List[Filter],
                     val partitionByLevelSetID: Boolean = true) extends Serializable

  object TDALens {

    /**
      * Vararg factory function.
      */
    def apply(filters: Filter*): TDALens = TDALens(filters.toList)

  }

  case object INFINITY extends Serializable

  sealed trait FilterSpec extends Serializable {
    def usesKNN = false
  }

  case class Feature(n: Int) extends FilterSpec

  case class PrincipalComponent(n: Int) extends FilterSpec

  case class LaplacianEigenVector(n: Int,
                                  k: Option[Int] = None,
                                  sigma: Double = DEFAULT_SIGMA,
                                  distance: DistanceFunction = DEFAULT_DISTANCE) extends FilterSpec {
    override def usesKNN = true
  }

  case class Eccentricity(p: Either[Int, _],
                          distance: DistanceFunction = DEFAULT_DISTANCE) extends FilterSpec

  case class Density(sigma: Double,
                     distance: DistanceFunction = DEFAULT_DISTANCE) extends FilterSpec

  implicit def toFilter(spec: FilterSpec): Filter = Filter(spec)

  case class Filter(val spec: FilterSpec,
                    val nrBins: Int = 20,
                    val overlap: Percentage = 0.25,
                    val sketch: Option[SketchParams] = None,
                    val balanced: Boolean = false) extends Serializable {

    require(nrBins > 0, "nrBins must be greater than 0")
    require(overlap >= 0, "overlap cannot be negative")
  }

  trait SimpleName {
    override def toString = getClass.getSimpleName.split("\\$").last
  }

}