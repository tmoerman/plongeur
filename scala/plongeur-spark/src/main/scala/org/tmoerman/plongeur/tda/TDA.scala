package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.apache.spark.{Logging, RangePartitioner}
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.{Clustering, SimpleSmileClusteringProvider}
import org.tmoerman.plongeur.util.IterableFunctions._
import shapeless.{::, HList, HNil}

/**
  * @author Thomas Moerman
  */
trait TDA extends Logging {

  def clusterLevelSets(lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams): RDD[(LevelSetID, (List[DataPoint], LocalClustering))] = {
    import ctx._

    val clusterer = SimpleSmileClusteringProvider

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val keyedByLevelSetId =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))

    val maybeCustomPartitioned =
      if (clusteringParams.partitionByLevelSetID)
        keyedByLevelSetId.partitionBy(new RangePartitioner(32, keyedByLevelSetId))
      else
        keyedByLevelSetId

    val rdd =
      maybeCustomPartitioned
        .groupByKey
        .mapValues(levelSetPoints => {
          val pointsList = levelSetPoints.toList
          (pointsList, clusterer.apply(pointsList, clusteringParams))
        })
        .cache

    rdd
  }

  def applyScale(levelSetClustersRDD: RDD[(LevelSetID, (List[DataPoint], LocalClustering))], scaleSelection: ScaleSelection) =
    levelSetClustersRDD
      .map { case (levelSetID, (clusterPoints, clustering)) => localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
      .cache

  def makeTDAResult(partitionedClustersRDD: RDD[List[Cluster]], collapseDuplicateClusters: Boolean): TDAResult = {
    lazy val duplicatesAllowed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(identity)

    lazy val duplicatesCollapsed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
        .reduceByKey((c1, c2) => c1) // if two clusters are keyed by an identical set of data points, retain only one of them
        .values

    val clustersRDD = (if (collapseDuplicateClusters) duplicatesCollapsed else duplicatesAllowed).cache // cache because it is used twice in 1 computation

    val clusterEdgesRDD =
      clustersRDD
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id))) // melt all clusters by points
        .groupByKey
        .values
        .flatMap(_.toSet.subsets(2))
        .distinct
        .cache

    // B< B< B< --- cut here --- merge in selections/active nodes --- colorings

    TDAResult(clustersRDD, clusterEdgesRDD)
  }

  def flattenTuple[A, B, C](t: ((A, B), C)) = t match {
    case ((a, b), c) => (a, b, c)
  }

}