package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.cluster.Clustering.{ScaleSelection, LocalClustering, ClusteringParams}
import org.tmoerman.plongeur.tda.cluster.{BroadcastSmileClusteringProvider, SimpleSmileClusteringProvider}
import rx.lang.scala.Observable
import Model._
import shapeless.{::, HNil, HList}

/**
  * @author Thomas Moerman
  */
object TDAMachine extends TDA {

  def run(tdaContext: TDAContext,
          tdaParams$: Observable[TDAParams]): Observable[(TDAParams, TDAResult)] = {

    // TDA computation merges in parameter changes

    val init: (TDAContext, Option[TDAParams]) = (tdaContext, None)

    val ctxParams$ =
      tdaParams$
        .scan(init){ case ((ctx, _), params) => (params.amend(ctx), Some(params)) }
        .distinctUntilChanged

    // keeping lens and context together because the lens updates the context's state.

    val lensCtx$             = ctxParams$.flatMapIterable{ case (ctx, opt) => opt.map(params => (params.lens, ctx)) }.distinctUntilChanged

    // deconstructing the applied parameters

    val appliedParams$       = ctxParams$.flatMapIterable(_._2)

    val clusteringParams$    = appliedParams$.map(_.clusteringParams         ).distinctUntilChanged
    val scaleSelection$      = appliedParams$.map(_.scaleSelection           ).distinctUntilChanged
    val collapseDuplicates$  = appliedParams$.map(_.collapseDuplicateClusters).distinctUntilChanged

    // combine the deconstructed parameter pieces with the computation

    val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets_P.tupled)
    val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale_P.tupled)
    val paramsWithResult$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult_P.tupled)

    paramsWithResult$
  }

  // TODO better naming of following functions

  val clusterLevelSets_P = (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    logDebug(s">>> clusterLevelSets $lens")

    val rdd: RDD[(LevelSetID, (List[DataPoint], LocalClustering))] = clusterLevelSets(lens, ctx, clusteringParams)

    (clusteringParams :: lens :: HNil, rdd)
  }

  val applyScale_P = (product: (HList, RDD[(LevelSetID, (List[DataPoint], LocalClustering))]), scaleSelection: ScaleSelection) => {
    logDebug(s">>> applyScale $scaleSelection")

    val (hlist, levelSetClustersRDD) = product

    val rdd = applyScale(levelSetClustersRDD, scaleSelection)

    (scaleSelection :: hlist, rdd)
  }

  val makeTDAResult_P = (product: (HList, RDD[List[Cluster]]), collapseDuplicateClusters: Boolean) => {
    logDebug(s">>> makeTDAResult - collapse duplicate clusters? $collapseDuplicateClusters")

    val (hlist, partitionedClustersRDD) = product

    val result: TDAResult = makeTDAResult(partitionedClustersRDD, collapseDuplicateClusters)

    val reconstructedParams = hlist match {
      case (scaleSelection: ScaleSelection) :: (clusteringParams: ClusteringParams) :: (lens: TDALens) :: HNil =>
        TDAParams(
          lens = lens,
          clusteringParams = clusteringParams,
          scaleSelection = scaleSelection,
          collapseDuplicateClusters = collapseDuplicateClusters)

      case _ => throw new IllegalStateException("dafuq?")
    }

    (reconstructedParams, result)
  }

}