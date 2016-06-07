package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Filters.toFilterFunction
import org.tmoerman.plongeur.tda.Model.{DataPoint, LevelSetID, Cluster, TDALens}
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClusteringProvider
import rx.lang.scala.Observable
import shapeless._

/**
  * @author Thomas Moerman
  */
object TDAMachine {

  val clusterLevelSets = (clusterer: LocalClusteringProvider) => (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    import ctx._
    import clusteringParams._

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val rdd =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .groupByKey
        .map { case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, distanceFunction, clusteringMethod)) }
        .cache

    (clusteringParams :: lens :: HNil, rdd)
  }

  val applyScale = (product: (HList, RDD[(LevelSetID, List[DataPoint], LocalClustering)]),
                    scaleSelection: ScaleSelection) => {

    val (hlist, levelSetClustersRDD) = product

    val rdd =
      levelSetClustersRDD
        .map{ case (levelSetID, clusterPoints, clustering) => localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
        .cache

    (scaleSelection :: hlist, rdd)
  }

  val makeTDAResult = (product: (HList, RDD[List[Cluster]]),
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

    val result = TDAResult(clustersRDD.cache, clusterEdgesRDD.cache)

    (reconstructedParams, result)
  }

  private def flattenTuple[A, B, C](t: ((A, B), C)) = t match {case ((a: A, b: B), c: C) => (a, b, c) }

  def tdaMachine(tdaParams$: Observable[TDAParams], ctx$: Observable[TDAContext]): Observable[(TDAParams, TDAResult)] = {

    val clusterer = SmileClusteringProvider // TODO inject

    // deconstructing the parameters

    val distinctTdaParams$      = tdaParams$.distinct

    val lens$                   = distinctTdaParams$.map(_.lens                     ).distinct //.onBackpressureLatest
    val clusteringParams$       = distinctTdaParams$.map(_.clusteringParams         ).distinct //.onBackpressureLatest
    val scaleSelection$         = distinctTdaParams$.map(_.scaleSelection           ).distinct //.onBackpressureLatest
    val collapseDuplicates$     = distinctTdaParams$.map(_.collapseDuplicateClusters).distinct //.onBackpressureLatest

    // TDA computation merges in parameter changes

    val lensCtx$             = lens$.combineLatestWith(ctx$)((lens, ctx) => (lens, lens.assocFilterMemos(ctx)) )

    val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets(clusterer).tupled)

    val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale.tupled)

    val paramsWithResult$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult.tupled)

    paramsWithResult$
  }

}
