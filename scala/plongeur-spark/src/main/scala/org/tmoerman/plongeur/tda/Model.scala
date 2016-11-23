package org.tmoerman.plongeur.tda

import java.util.UUID

import com.softwaremill.quicklens
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Colour.{Colouring, Nop}
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
         meta: Map[String, Any]) = DataPoint(index.toInt, features, Some(meta))

  case class DataPoint(val index: Index,
                       val features: MLVector,
                       val meta: Option[Map[String, Any]] = None) extends Serializable

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

  type FilterKey = FilterSpec
  type CacheKey = Serializable

  type ContextAmendment = TDAContext => TDAContext

  trait ContextLike {
    def N: Int
    def D: Int
    def dataPoints: RDD[DataPoint]
  }

  type Broadcasts  = Map[CacheKey, Broadcast[_]]
  type SketchCache = Map[CacheKey, Sketch]
  type KNNCache    = Map[CacheKey, KNN_RDD]
  type FilterCache = Map[FilterKey, FilterRDDFactory]

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

    def setScaleResolution(resolution: Int) =
      (params: TDAParams) => params.copy(scaleSelection = params.scaleSelection match {
        case histogram@HistogramScaleSelection(_) => histogram.copy(nrBins = resolution)
        case firstGap@FirstGapScaleSelection(_)   => firstGap.copy(gapPct = resolution)
        case danifold@DanifoldScaleSelection(_)   => danifold.copy(nrBins = resolution)
        case x: ScaleSelection => x
      })

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

    /**
      * Mechanism for computing cache keys:
      *
      * For some filter functions (PCA, Laplacian), an intermediate cached data structure is computed that is used in
      * different instances of the actual filter function: the n-th vector or principal component is picked out of the
      * same cached data structure. Therefore the cache key needs to be equal among those filter function instances.
      * Hence the motivation of setting the n variable to magic number -1.
      *
      * @return Returns a cache key for the specified filter.
      */
    def key: FilterKey = this

    val COMMON_KEY_n = -1
  }

  // TODO move this to Filters

  case class Meta(name: String) extends FilterSpec

  case class Feature(n: Int) extends FilterSpec

  case object FeatureMin      extends FilterSpec
  case object FeatureMax      extends FilterSpec
  case object FeatureMean     extends FilterSpec
  case object FeatureStDev    extends FilterSpec
  case object FeatureVariance extends FilterSpec

  case class PrincipalComponent(n: Int) extends FilterSpec {
    override def key = this.copy(n = COMMON_KEY_n)
  }

  case class LaplacianEigenVector(n: Int,
                                  k: Option[Int] = None,
                                  sigma: Double = DEFAULT_SIGMA,
                                  distance: DistanceFunction = DEFAULT_DISTANCE) extends FilterSpec {
    override def usesKNN = true
    override def key = this.copy(n = COMMON_KEY_n)
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