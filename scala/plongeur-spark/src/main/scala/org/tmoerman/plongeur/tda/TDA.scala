package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Model.{DataPoint, _}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * TODO turn into a part of the reactive machinery with observable input and observable output. Cfr. Cycle.js
  *
  * @author Thomas Moerman
  */
object TDA {

  val clusterer = SmileClusteringProvider // TODO injectable

  def apply(tdaParams: TDAParams, tdaContext: TDAContext): TDAResult = {
    import tdaContext._
    import tdaParams._
    import tdaParams.clusteringParams._

    val levelSetsInverse =
      coveringBoundaries
        .orElse(Some(boundaries(lens.functions, dataPoints)))
        .map(boundaries => levelSetsInverseFunction(lens, boundaries))
        .get

    val tripletsRDD =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .groupByKey // TODO turn this into a reduceByKey with an incremental single linkage algorithm? -> probably pointless
        .map{ case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, distanceFunction, clusteringMethod)) }
        .cache

    val partitionedClustersRDD: RDD[List[Cluster]] =
      tripletsRDD
        .map{ case (levelSetID, clusterPoints, clustering) =>
          localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }

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
        .groupByKey
        .values
        .flatMap(_.toSet.subsets(2))
        .distinct
        .cache

    TDAResult(clustersRDD, clusterEdgesRDD)
  }

  implicit def toTDAContext(rdd: RDD[DataPoint]): TDAContext = new TDAContext(rdd)

}

case class TDAContext(val dataPoints: RDD[DataPoint]) extends Serializable {

  lazy val N = dataPoints.count

}

case class TDAParams(val lens: TDALens,
                     val clusteringParams: ClusteringParams,
                     val coveringBoundaries: Option[Boundaries] = None) extends Serializable

case class TDAResult(val clustersRDD: RDD[Cluster], val edgesRDD: RDD[Set[ID]]) extends Serializable {

  lazy val clusters = clustersRDD.collect

  lazy val edges = edgesRDD.collect

}
