package org.tmoerman.plongeur.tda

import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.PCA
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model.{DataPoint, _}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import org.tmoerman.plongeur.util.IterableFunctions._
import rx.lang.scala.Observable

/**
  * TODO turn into a part of the reactive machinery with observable input and observable output. Cfr. Cycle.js
  *
  * @author Thomas Moerman
  */
object TDA {

  val clusterer = SmileClusteringProvider // TODO injectable

  def doMain(in: Observable[String]) = {
    val out: Observable[Int] = in.map(_.length)
    out
  }

  def echo(call: String) = s"$call $call"

  def apply(tdaParams: TDAParams, tdaContext: TDAContext): TDAResult = {

//    import tdaContext._
//    import tdaParams._
//    import tdaParams.clusteringParams._

    val filterFunctions = tdaParams.lens.filters.map(f => toFilterFunction(f.spec, tdaContext))

    //val boundaries = coveringBoundaries.getOrElse(calculateBoundaries(filterFunctions, dataPoints))
    val boundaries = calculateBoundaries(filterFunctions, tdaContext.dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, tdaParams.lens, filterFunctions)

    val byLevelSet =
      tdaContext
        .dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .groupByKey // TODO turn this into a reduceByKey with an incremental single linkage algorithm? -> probably pointless

    val tripletsRDD =
      byLevelSet
        .map{ case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, tdaParams.clusteringParams.distanceFunction,
                                                                                    tdaParams.clusteringParams.clusteringMethod)) }
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

    val clustersRDD = (if (tdaParams.collapseDuplicateClusters) duplicatesCollapsed else duplicatesAllowed)

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

// TODO: the TDA context is an observable of memoized auziliary data structures
// TODO: it changes in function of selected specs in the TDA configurations
//
// Heuristic: whenever there is mutable state in a data system, consider modeling
// it as changes propagated through an observable !!!

case class TDAContext(val sc: SparkContext, val dataPoints: RDD[DataPoint]) extends Serializable {

  lazy val N = dataPoints.count

  lazy val pca = new PCA(3).fit(dataPoints.map(_.features))

}

case class TDAParams(val lens: TDALens,
                     val clusteringParams: ClusteringParams,
                     val collapseDuplicateClusters: Boolean = true,
                     val scaleSelection: ScaleSelection = histogram(),
                     val coveringBoundaries: Option[Boundaries] = None) extends Serializable

case class TDAResult(val clustersRDD: RDD[Cluster], val edgesRDD: RDD[Set[ID]]) extends Serializable {

  lazy val clusters = clustersRDD.collect

  lazy val edges = edgesRDD.collect

}
