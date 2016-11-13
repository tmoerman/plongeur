package org.tmoerman.plongeur.tda

import org.apache.spark.RangePartitioner
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Colour.Colouring
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SimpleSmileClusteringProvider
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
trait TDA {

  def createLevelSets(lens: TDALens, ctx: TDAContext): RDD[(LevelSetID, DataPoint)] = {
    val levelSetsRDD = levelSetInverseRDD(ctx, lens)

    val result = // TODO where does this belong?
      if (lens.partitionByLevelSetID)
        levelSetsRDD.partitionBy(new RangePartitioner(ctx.sc.defaultParallelism, levelSetsRDD))
      else
        levelSetsRDD

    result.cache
  }

  def clusterLevelSets(levelSetsRDD: RDD[(LevelSetID, DataPoint)],
                       clusteringParams: ClusteringParams,
                       clusterer: LocalClusteringProvider = SimpleSmileClusteringProvider): RDD[(LevelSetID, (List[DataPoint], LocalClustering))] =
    levelSetsRDD
      .groupByKey
      .mapValues(levelSetPoints => {
        val pointsList = levelSetPoints.toList
        (pointsList, clusterer.apply(pointsList, clusteringParams))
      })
      .cache


  def formClusters(partitionedClustersRDD: RDD[List[Cluster]], collapseDuplicateClusters: Boolean) = {
    lazy val duplicatesAllowed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(identity)

    lazy val duplicatesCollapsed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
        .reduceByKey((c1, c2) => c1) // if two clusters are keyed by an identical set of data points, retain only one of them
        .values

    val clustersRDD = (if (collapseDuplicateClusters) duplicatesCollapsed else duplicatesAllowed).cache // cache because it is used twice in 1 computation

    lazy val edgesRDD =
      clustersRDD
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, (cluster.id, cluster.size))))
        .groupByKey // (dp1, [(cl1, 2), (cl2, 5), (cl3, 7)])
        .values
        .flatMap(_.toSet.subsets(2).map(_.toSeq.sortBy(- _._2))) // order by cluster size (hi -> lo)
        .distinct
        .map(_.map(_._1)) // retain only cluster ids
        .cache

    (clustersRDD, edgesRDD)
  }

  def applyScale(levelSetClustersRDD: RDD[(LevelSetID, (List[DataPoint], LocalClustering))], scaleSelection: ScaleSelection) =
    levelSetClustersRDD
      .map { case (levelSetID, (clusterPoints, clustering)) => localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
      .cache

  def applyColouring(clustersRDD: RDD[Cluster], edgesRDD: RDD[ClusterEdge], colouring: Colouring, ctx: TDAContext) = {
    val colouredClustersRDD = colouring(ctx)(clustersRDD).cache

    TDAResult(colouredClustersRDD, edgesRDD)
  }

  def flattenTuple[A, B, C](t: ((A, B), C)) = t match {
    case ((a, b), c) => (a, b, c)
  }

}