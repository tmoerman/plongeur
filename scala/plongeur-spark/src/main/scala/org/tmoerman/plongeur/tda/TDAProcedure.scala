package org.tmoerman.plongeur.tda

import org.apache.spark.RangePartitioner
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.BroadcastSmileClusteringProvider
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  *
  *
  * @author Thomas Moerman
  */
object TDAProcedure extends TDA {

  // TODO refactor to use the functions from the TDA trait
  def apply(tdaParams: TDAParams, ctx: TDAContext): TDAResult = {

    val amendedCtx = tdaParams.amend(ctx)

    val clusterer = new BroadcastSmileClusteringProvider(amendedCtx.broadcasts)

    val filterFunctions = tdaParams.lens.filters.map(f => toFilterFunction(f.spec, amendedCtx))

    val boundaries = calculateBoundaries(filterFunctions, amendedCtx.dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, tdaParams.lens, filterFunctions)

    //import org.tmoerman.plongeur.util.IterableFunctions._

    val keyedByLevelSet =
      amendedCtx
        .dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))

    val groupedByLevelSet =
      keyedByLevelSet
        .partitionBy(new RangePartitioner(8, keyedByLevelSet))
        .groupByKey // TODO turn this into a reduceByKey with an incremental single linkage algorithm? -> probably pointless

    val tripletsRDD =
      groupedByLevelSet
        .map{ case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, tdaParams.clusteringParams)) }
        .cache

    val partitionedClustersRDD: RDD[List[Cluster]] =
      tripletsRDD
        .map{ case (levelSetID, clusterPoints, clustering) =>
          localClusters(levelSetID, clusterPoints, clustering.labels(tdaParams.scaleSelection)) }

    lazy val duplicatesAllowed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(identity)

    lazy val duplicatesCollapsed: RDD[Cluster] =
      partitionedClustersRDD
        .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
        .reduceByKey((c1, c2) => c1)
        .values

    val clustersRDD = (if (tdaParams.collapseDuplicateClusters) duplicatesCollapsed else duplicatesAllowed).cache

    val clusterEdgesRDD =
      clustersRDD
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id)))   // melt all clusters by points
        .groupByKey
        .values
        .flatMap(_.toSet.subsets(2))
        .distinct
        .cache

    TDAResult(clustersRDD, clusterEdgesRDD)
  }

}