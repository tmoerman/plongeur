package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.apache.spark.{Logging, RangePartitioner}
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import shapeless.{::, HList, HNil}

/**
  * @author Thomas Moerman
  */
trait TDA extends Logging {

  val clusterLevelSets = (clusterer: LocalClusteringProvider) => (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    import ctx._
    import org.tmoerman.plongeur.util.IterableFunctions._

    logDebug(s">>> clusterLevelSets $lens")

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val keyedByLevelSetId =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))

    val groupedByLevelSetId =
      if (clusteringParams.partitionByLevelSetID)
        keyedByLevelSetId
          .partitionBy(new RangePartitioner(8, keyedByLevelSetId))
          .groupByKey
      else
        keyedByLevelSetId
          .groupByKey

    val rdd =
      groupedByLevelSetId
        .map { case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toList, clusteringParams))
        }
        .cache

    (clusteringParams :: lens :: HNil, rdd)
  }

  val applyScale = (product: (HList, RDD[(LevelSetID, List[DataPoint], LocalClustering)]),
                    scaleSelection: ScaleSelection) => {

    logDebug(s">>> applyScale $scaleSelection")

    val (hlist, levelSetClustersRDD) = product

    val rdd =
      levelSetClustersRDD
        .map { case (levelSetID, clusterPoints, clustering) => localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
        .cache

    (scaleSelection :: hlist, rdd)
  }

  val makeTDAResult = (product: (HList, RDD[List[Cluster]]),
                       collapseDuplicateClusters: Boolean) => {

    logDebug(s">>> makeTDAResult - collapse duplicate clusters? $collapseDuplicateClusters")

    val (hlist, partitionedClustersRDD) = product

    lazy val duplicatesAllowed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(identity)

    lazy val duplicatesCollapsed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
        .reduceByKey((c1, c2) => c1)
        .values

    val clustersRDD = (if (collapseDuplicateClusters) duplicatesCollapsed else duplicatesAllowed).cache

    val clusterEdgesRDD =
      clustersRDD
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id))) // melt all clusters by points
        .groupByKey
        .values
        .flatMap(_.toSet.subsets(2))
        .distinct
        .cache

    val reconstructedParams = hlist match {
      case (scaleSelection: ScaleSelection) :: (clusteringParams: ClusteringParams) :: (lens: TDALens) :: HNil =>
        TDAParams(
          lens = lens,
          clusteringParams = clusteringParams,
          scaleSelection = scaleSelection,
          collapseDuplicateClusters = collapseDuplicateClusters)
    }

    val result = TDAResult(clustersRDD, clusterEdgesRDD)

    (reconstructedParams, result)
  }

  def flattenTuple[A, B, C](t: ((A, B), C)) = t match {
    case ((a, b), c) => (a, b, c)
  }

}