package org.tmoerman.plongeur.tda

import org.apache.spark.SparkContext
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters.toFilterFunction
import org.tmoerman.plongeur.tda.Model.{Cluster, TDALens}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import rx.lang.scala.Observable

/**
  * @author Thomas Moerman
  */
object RxTDA {

  def tdaMachine(tdaParams$: Observable[TDAParams],
                 ctx$:    Observable[TDAContext])
                (implicit sc: SparkContext): Observable[TDAResult] = {

    val clusterer = SmileClusteringProvider // TODO inject

    // deconstructing the parameters

    val lens$: Observable[TDALens] = tdaParams$.map(_.lens).distinct

    val scaleSelection$: Observable[ScaleSelection] = tdaParams$.map(_.clusteringParams.scaleSelection).distinct

    val clusteringParams$: Observable[ClusteringParams] = tdaParams$.map(_.clusteringParams).distinct

    val collapseDuplicates$: Observable[Boolean] = clusteringParams$.map(_.collapseDuplicateClusters).distinct

    // TDA computation merges in parameter changes

    val lensCtx$: Observable[(TDALens, TDAContext)] =
      (lens$ combineLatest ctx$)
        .map{ case (lens, ctx) => (lens, lens.assocFilterMemos(ctx)) }

    val byLevelSetRDD$ =
      lensCtx$
        .map{ case (lens, ctx) => {
          import ctx._

          val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

          val boundaries = calculateBoundaries(filterFunctions, dataPoints)

          val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

          val byLevelSetRDD =
            dataPoints
              .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
              .groupByKey

          byLevelSetRDD.cache
        }}

    val tripleRDD$ =
      (byLevelSetRDD$ combineLatest clusteringParams$)
        .map{ case (byLevelSet, clusteringParams) => {
          import clusteringParams._

          val tripleRDD =
            byLevelSet
              .map { case (levelSetID, levelSetPoints) =>
                (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, distanceFunction, clusteringMethod)) }

          tripleRDD.cache
        }}

    val partitionedClustersRDD$ =
      (tripleRDD$ combineLatest scaleSelection$)
        .map{ case (tripleRDD, scaleSelection) => {

          val partitionedClustersRDD =
            tripleRDD.map{ case (levelSetID, clusterPoints, clustering) =>
              localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }

          partitionedClustersRDD.cache
        }}

    val tdaResult$ =
      (partitionedClustersRDD$ combineLatest collapseDuplicates$)
        .map{ case (partitionedClustersRDD, collapseDuplicateClusters) => {

          val clustersRDD =
            if (collapseDuplicateClusters)
              partitionedClustersRDD
                .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
                .reduceByKey((c1, c2) => c1)
                .values
            else
              partitionedClustersRDD
                .flatMap(identity)

          val clusterEdgesRDD =
            clustersRDD
              .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id)))   // melt all clusters by points
              .groupByKey
              .values
              .flatMap(_.toSet.subsets(2))
              .distinct
              .cache

          TDAResult(clustersRDD, clusterEdgesRDD)
        }}

    tdaResult$
  }

  def launch(file: String): Observable[TDAResult] = {

    ???
  }

}
