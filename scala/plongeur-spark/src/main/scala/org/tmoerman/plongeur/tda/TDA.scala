package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.apache.spark.{RangePartitioner}
import org.tmoerman.plongeur.tda.Colour.Colouring
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
trait TDA {

  def createLevelSets(lens: TDALens, ctx: TDAContext): RDD[(LevelSetID, DataPoint)] = {
    import ctx._

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val levelSetsRDD =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))

    // TODO where does this belong?
    val result =
      if (lens.partitionByLevelSetID)
        levelSetsRDD.partitionBy(new RangePartitioner(32, levelSetsRDD))
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
    import colouring._

    val broadcasts = ctx.broadcasts

    val colouredClustersRDD =
      clustersRDD
        .map(cluster => cluster.copy(colours = palette.toSeq.flatMap(rgbs => strategy.toBinner(broadcasts).apply(cluster).map(bin => rgbs(bin)))))

    TDAResult(colouredClustersRDD, edgesRDD)
  }

  def flattenTuple[A, B, C](t: ((A, B), C)) = t match {
    case ((a, b), c) => (a, b, c)
  }

}