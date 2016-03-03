package org.tmoerman.plongeur.tda

import org.apache.spark.Partitioner._
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model.{DataPoint, _}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import org.tmoerman.plongeur.util.IterableFunctions._

import scala.reflect.ClassTag

/**
  * @author Thomas Moerman
  */
object TDA {

  def execute[ID](lens: Lens,
                  dataRDD: RDD[DataPoint],
                  coveringBoundaries: Option[Array[(Double, Double)]] = None,
                  distanceFunction: DistanceFunction = euclidean,
                  clusteringMethod: ClusteringMethod = SINGLE,
                  scaleSelection: ScaleSelection = histogram(),
                  clusteringProvider: LocalClusteringProvider = SmileClusteringProvider,
                  clusterIdentifier: ClusterIDGenerator[ID],
                  collapseDuplicateClusters: Boolean = true)
                 (implicit tag: ClassTag[ID]): TDAResult[ID] = {

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

    val partitionedClustersRDD: RDD[List[Cluster[ID]]] =
      tripletsRDD
        .map{ case (levelSetID, dataPoints, clustering) =>
          localClusters(levelSetID, dataPoints, clustering.labels(scaleSelection), clusterIdentifier) }

    lazy val duplicatesAllowed: RDD[Cluster[ID]] =
      partitionedClustersRDD
        .flatMap(identity)

    lazy val duplicatesCollapsed: RDD[Cluster[ID]] =
      partitionedClustersRDD
        .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
        .reduceByKey((c1, c2) => c1)
        .values

    val clustersRDD = (if (collapseDuplicateClusters) duplicatesCollapsed else duplicatesAllowed).cache

    val clusterEdgesRDD: RDD[Set[ID]] =
      clustersRDD
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id)))   // melt all clusters by points
        .combineByKey((clusterID: ID) => Set(clusterID),                                  // TODO turn into groupByKey?
        (acc: Set[ID], clusterID: ID) => acc + clusterID,
        (acc1: Set[ID], acc2: Set[ID]) => acc1 ++ acc2)   // create proto-clusters, collapse doubles
        .values
        .flatMap(_.subsets(2))
        .distinct
        .cache

    TDAResult(boundaries, clustersRDD, clusterEdgesRDD)
  }

  case class TDAResult[ID](val bounds: Array[(Double, Double)],
                           val clustersRDD: RDD[Cluster[ID]],
                           val edgesRDD: RDD[Set[ID]]) extends Serializable {

    lazy val clusters = clustersRDD.collect

    lazy val edges = edgesRDD.collect

  }

}
