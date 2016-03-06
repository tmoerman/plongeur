package org.tmoerman.plongeur.tda

import org.apache.spark.Partitioner._
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Model.{DataPoint, _}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import org.tmoerman.plongeur.util.IterableFunctions._

import scala.reflect.ClassTag

/**
  * TODO turn into a part of the reactive machinery with observable input and observable output. Cfr. Cycle.js
  *
  * @author Thomas Moerman
  */
object TDA {

  case class TDAParams(val lens: Lens,
                       val clusteringParams: ClusteringParams,
                       val coveringBoundaries: Option[Array[(Double, Double)]] = None) extends Serializable

  def execute(dataRDD: RDD[DataPoint],
              tdaParams: TDAParams,
              clusteringProvider: LocalClusteringProvider = SmileClusteringProvider): TDAResult = {

    import tdaParams._
    import tdaParams.clusteringParams._

    val boundaries = coveringBoundaries.getOrElse(toBoundaries(lens.functions, dataRDD))

    val levelSetInverse = toLevelSetInverseFunction(lens, boundaries)

    val tripletsRDD =
      dataRDD
        .flatMap(dataPoint => levelSetInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .repartitionAndSortWithinPartitions(defaultPartitioner(dataRDD)) // TODO which partitioner?
        .mapPartitions(
          _
            .view                                                              // TODO view instead of toIterable ?
            .groupRepeats(selector = { case (levelSetID, _) => levelSetID })   // TODO a more efficient streaming version with selector and extractor
            .map(pairs => {
              val levelSetID = pairs.head._1
              val dataPoints = pairs.map(_._2)
              val clustering = clusteringProvider.apply(dataPoints, distanceFunction, clusteringMethod)

              (levelSetID, dataPoints, clustering)
            }))
        .cache

    val partitionedClustersRDD: RDD[List[Cluster]] =
      tripletsRDD
        .map{ case (levelSetID, dataPoints, clustering) =>
          localClusters(levelSetID, dataPoints, clustering.labels(scaleSelection)) }

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
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id)))   // melt all clusters by points
        .combineByKey(
          (clusterID: ID)                => Set(clusterID),                               // TODO turn into groupByKey?
          (acc: Set[ID], clusterID: ID)  => acc + clusterID,
          (acc1: Set[ID], acc2: Set[ID]) => acc1 ++ acc2)
        .values
        .flatMap(_.subsets(2))
        .distinct
        .cache

    TDAResult(boundaries, clustersRDD, clusterEdgesRDD)
  }

  case class TDAResult(val bounds: Array[(Double, Double)],
                       val clustersRDD: RDD[Cluster],
                       val edgesRDD: RDD[Set[ID]]) extends Serializable {

    lazy val clusters = clustersRDD.collect

    lazy val edges = edgesRDD.collect

  }

}
