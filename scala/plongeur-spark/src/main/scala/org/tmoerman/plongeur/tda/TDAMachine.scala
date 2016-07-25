package org.tmoerman.plongeur.tda

import org.apache.spark.Logging
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
object TDAMachine extends Logging {

  private val clusterLevelSets = (clusterer: LocalClusteringProvider) => (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    import ctx._

    logDebug(s">>> clusterLevelSets $lens")

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val rdd =
      dataPoints
        .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .groupByKey
        .map { case (levelSetID, levelSetPoints) =>
          (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toList, clusteringParams)) }
        .cache

    (clusteringParams :: lens :: HNil, rdd)
  }

  private val applyScale = (product: (HList, RDD[(LevelSetID, List[DataPoint], LocalClustering)]),
                            scaleSelection: ScaleSelection) => {

    logDebug(s">>> applyScale $scaleSelection")

    val (hlist, levelSetClustersRDD) = product

    val rdd =
      levelSetClustersRDD
        .map{ case (levelSetID, clusterPoints, clustering) => localClusters(levelSetID, clusterPoints, clustering.labels(scaleSelection)) }
        .cache

    (scaleSelection :: hlist, rdd)
  }

  private val makeTDAResult = (product: (HList, RDD[List[Cluster]]),
                               collapseDuplicateClusters: Boolean) => {

    logDebug(s">>> makeTDAResult - collapse duplicate clusters? $collapseDuplicateClusters")

    val (hlist, partitionedClustersRDD) = product

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

    val reconstructedParams = hlist match {
      case (scaleSelection: ScaleSelection) :: (clusteringParams: ClusteringParams) :: (lens: TDALens) :: HNil =>
        TDAParams(
          lens                      = lens,
          clusteringParams          = clusteringParams,
          scaleSelection            = scaleSelection,
          collapseDuplicateClusters = collapseDuplicateClusters)
    }

    val result = TDAResult(clustersRDD, clusterEdgesRDD)

    (reconstructedParams, result)
  }

  private def flattenTuple[A, B, C](t: ((A, B), C)) = t match { case ((a, b), c) => (a, b, c) }

  def run(ctx: TDAContext, tdaParams$: Observable[TDAParams]): Observable[(TDAParams, TDAResult)] = {

    val clusterer = SmileClusteringProvider // TODO inject

    // source observable with backpressure

    val tdaParamsSource$ = tdaParams$

    // deconstructing the parameters

    val lens$               = tdaParamsSource$.map(_.lens                     ).distinctUntilChanged
    val clusteringParams$   = tdaParamsSource$.map(_.clusteringParams         ).distinctUntilChanged
    val scaleSelection$     = tdaParamsSource$.map(_.scaleSelection           ).distinctUntilChanged
    val collapseDuplicates$ = tdaParamsSource$.map(_.collapseDuplicateClusters).distinctUntilChanged

    // TDA computation merges in parameter changes

    val ctx$                 = lens$.scan(ctx){(ctx, lens) => lens.assocFilterMemos(ctx)}.distinctUntilChanged

    val lensCtx$             = lens$.combineLatest(ctx$)

    val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets(clusterer).tupled)

    val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale.tupled)

    val paramsWithResult$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult.tupled)

    paramsWithResult$
  }

}