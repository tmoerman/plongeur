package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Colour.Colouring
import org.tmoerman.plongeur.tda.cluster.Clustering.{ScaleSelection, LocalClustering, ClusteringParams}
import org.tmoerman.plongeur.tda.cluster.SimpleSmileClusteringProvider
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
    val colouring$           = appliedParams$.map(_.colouring                ).distinctUntilChanged

    // combine the deconstructed parameter pieces with the computation


    val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets_P.tupled)
    val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale_P.tupled)
    val clustersAndEdges$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(formClusters_P.tupled)
    val paramsAndResult$     = clustersAndEdges$.combineLatest(colouring$).map(applyColouring_P.tupled)

    paramsAndResult$
  }

  // TODO better naming of following functions

  val clusterLevelSets_P = (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    logDebug(s">>> clusterLevelSets $lens")

    val levelSets = createLevelSets(lens, ctx) // TODO factor out this step

    val rdd: RDD[(LevelSetID, (List[DataPoint], LocalClustering))] = clusterLevelSets(levelSets, clusteringParams)

    (clusteringParams :: lens :: HNil, rdd, ctx)
  }

  val applyScale_P = (product: (HList, RDD[(LevelSetID, (List[DataPoint], LocalClustering))], TDAContext), scaleSelection: ScaleSelection) => {
    logDebug(s">>> applyScale $scaleSelection")

    val (hlist, levelSetClustersRDD, ctx) = product

    val rdd = applyScale(levelSetClustersRDD, scaleSelection)

    (scaleSelection :: hlist, rdd, ctx)
  }

  val formClusters_P = (product: (HList, RDD[List[Cluster]], TDAContext), collapseDuplicateClusters: Boolean) => {
    logDebug(s">>> formCluster - collapse duplicate clusters? $collapseDuplicateClusters")

    val (hlist, partitionedClustersRDD, ctx) = product

    val (clustersRDD, edgesRDD) = formClusters(partitionedClustersRDD, collapseDuplicateClusters)

    (collapseDuplicateClusters :: hlist, (clustersRDD, edgesRDD), ctx)
  }

  val applyColouring_P = (product: (HList, (RDD[Cluster], RDD[ClusterEdge]), TDAContext), colouring: Colouring) => {
    logDebug(s">>> applyColouring $colouring")

    val (hlist, (clustersRDD, edgesRDD), ctx) = product

    val result = applyColouring(clustersRDD, edgesRDD, colouring, ctx)

    val reconstructedParams = hlist match {
      case
        (collapseDuplicateClusters: Boolean) ::
          (scaleSelection: ScaleSelection) ::
          (clusteringParams: ClusteringParams) ::
          (lens: TDALens) :: HNil =>
        TDAParams(
          lens = lens,
          clusteringParams = clusteringParams,
          scaleSelection = scaleSelection,
          collapseDuplicateClusters = collapseDuplicateClusters,
          colouring = colouring)

      case _ => throw new Exception("dafuq?")
    }

    (reconstructedParams, result)
  }

}