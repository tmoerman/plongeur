package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters.toFilterFunction
import org.tmoerman.plongeur.tda.Model.{DataPoint, LevelSetID, Cluster, TDALens}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import rx.lang.scala.Observable

/**
  * @author Thomas Moerman
  */
object RxTDA {

  val clusterLevelSets = (clusterer: LocalClusteringProvider) => (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    import ctx._
    import clusteringParams._

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    dataPoints
      .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
      .groupByKey
      .map { case (levelSetID, levelSetPoints) =>
        (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, distanceFunction, clusteringMethod)) }
      .cache
  }

  val applyScale = (tripleRDD: RDD[(LevelSetID, List[DataPoint], LocalClustering)],
                    scaleSelection: ScaleSelection) => {
    tripleRDD
      .map{ case (levelSetID, clusterPoints, clustering) =>
        localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
      .cache
  }

  val makeTDAResult = (partitionedClustersRDD: RDD[List[Cluster]], collapseDuplicateClusters: Boolean) => {
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

    TDAResult(clustersRDD.cache, clusterEdgesRDD.cache)
  }

  def flattenTuple[A, B, C](t: ((A, B), C)) = t match {case ((a: A, b: B), c: C) => (a, b, c) }

  def tdaMachine(tdaParams$: Observable[TDAParams], ctx$: Observable[TDAContext]): Observable[TDAResult] = {

    val clusterer = SmileClusteringProvider // TODO inject

    // deconstructing the parameters

    val distinctTdaParams$      = tdaParams$.distinct

    val lens$                   = distinctTdaParams$ map(_.lens)                      distinct
    val clusteringParams$       = distinctTdaParams$ map(_.clusteringParams)          distinct
    val scaleSelection$         = distinctTdaParams$ map(_.scaleSelection)            distinct
    val collapseDuplicates$     = distinctTdaParams$ map(_.collapseDuplicateClusters) distinct

    // TDA computation merges in parameter changes

    val lensCtx$     = lens$.combineLatest(ctx$).map{ case (lens, ctx) => (lens, lens.assocFilterMemos(ctx)) }

    val tripleRDD$   = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets(clusterer).tupled)

    val clustersRDD$ = tripleRDD$.combineLatest(scaleSelection$).map(applyScale.tupled)

    val tdaResult$   = clustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult.tupled)

    tdaResult$
  }

}
