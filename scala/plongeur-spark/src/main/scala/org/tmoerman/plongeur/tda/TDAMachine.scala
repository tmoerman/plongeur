package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters.toFilterFunction
import org.tmoerman.plongeur.tda.Model.{Cluster, DataPoint, LevelSetID, TDALens}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import rx.lang.scala.Observable
import shapeless._

/**
  * @author Thomas Moerman
  */
object TDAMachine {

  private val clusterLevelSets = (clusterer: LocalClusteringProvider) => (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    import clusteringParams._
    import ctx._

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val rdd =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .groupByKey
        .map { case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toList, distanceFunction, clusteringMethod)) }
        .cache

    (clusteringParams :: lens :: HNil, rdd)
  }

  private val applyScale = (product: (HList, RDD[(LevelSetID, List[DataPoint], LocalClustering)]),
                            scaleSelection: ScaleSelection) => {

    val (hlist, levelSetClustersRDD) = product

    val rdd =
      levelSetClustersRDD
        .map{ case (levelSetID, clusterPoints, clustering) => localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
        .cache

    (scaleSelection :: hlist, rdd)
  }

  private val makeTDAResult = (product: (HList, RDD[List[Cluster]]),
                               collapseDuplicateClusters: Boolean) => {

    val (hlist, localClustersRDD) = product

    val clustersRDD =
      if (collapseDuplicateClusters)
        localClustersRDD
          .flatMap(_.map(cluster => (cluster.dataPoints, cluster)))
          .reduceByKey((c1, c2) => c1)
          .values
      else
        localClustersRDD
          .flatMap(identity)

    val clusterEdgesRDD =
      clustersRDD
        .flatMap(cluster => cluster.dataPoints.map(point => (point.index, cluster.id)))   // melt all clusters by points
        .groupByKey
        .values
        .flatMap(_.toSet.subsets(2))
        .distinct

    val reconstructedParams = hlist match {
      case (scaleSelection: ScaleSelection) :: (clusteringParams: ClusteringParams) :: (lens: TDALens) :: HNil =>
        TDAParams(
          lens                      = lens,
          clusteringParams          = clusteringParams,
          scaleSelection            = scaleSelection,
          collapseDuplicateClusters = collapseDuplicateClusters) }

    val result = TDAResult(clustersRDD, clusterEdgesRDD)

    (reconstructedParams, result)
  }

  private def flattenTuple[A, B, C](t: ((A, B), C)) = t match {case ((a: A, b: B), c: C) => (a, b, c) }

  def run(ctx: TDAContext, tdaParams$: Observable[TDAParams]): Observable[(TDAParams, TDAResult)] = {

    val clusterer = SmileClusteringProvider // TODO inject

    // source observable with backpressure

    val tdaParamsSource$ = tdaParams$.distinct.onBackpressureLatest

    // deconstructing the parameters

    val lens$               = tdaParamsSource$.map(_.lens                     ).distinct
    val clusteringParams$   = tdaParamsSource$.map(_.clusteringParams         ).distinct
    val scaleSelection$     = tdaParamsSource$.map(_.scaleSelection           ).distinct
    val collapseDuplicates$ = tdaParamsSource$.map(_.collapseDuplicateClusters).distinct

    // TDA computation merges in parameter changes

    val ctx$                 = Observable.just(ctx)

    val lensCtx$             = lens$.combineLatestWith(ctx$)((lens, ctx) => (lens, lens.assocFilterMemos(ctx)) )

    val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets(clusterer).tupled)

    val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale.tupled)

    val paramsWithResult$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult.tupled)

    paramsWithResult$
  }

}