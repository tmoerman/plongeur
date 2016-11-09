package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Colour.Colouring
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering.{ClusteringParams, LocalClustering, ScaleSelection}
import rx.lang.scala.Observable

/**
  * @author Thomas Moerman
  */
object TDAMachine extends TDA {

  def run(tdaContext: TDAContext,
          tdaParams$: Observable[TDAParams]): Observable[TDAResult] = {

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
    val levelSets = createLevelSets(lens, ctx) // TODO factor out this step

    val rdd: RDD[(LevelSetID, (List[DataPoint], LocalClustering))] = clusterLevelSets(levelSets, clusteringParams)

    (rdd, ctx)
  }

  val applyScale_P = (product: (RDD[(LevelSetID, (List[DataPoint], LocalClustering))], TDAContext), scaleSelection: ScaleSelection) => {
    val (levelSetClustersRDD, ctx) = product

    val rdd = applyScale(levelSetClustersRDD, scaleSelection)

    (rdd, ctx)
  }

  val formClusters_P = (product: (RDD[List[Cluster]], TDAContext), collapseDuplicateClusters: Boolean) => {
    val (partitionedClustersRDD, ctx) = product

    val (clustersRDD, edgesRDD) = formClusters(partitionedClustersRDD, collapseDuplicateClusters)

    ((clustersRDD, edgesRDD), ctx)
  }

  val applyColouring_P = (product: ((RDD[Cluster], RDD[ClusterEdge]), TDAContext), colouring: Colouring) => {
    val ((clustersRDD, edgesRDD), ctx) = product

    val result = applyColouring(clustersRDD, edgesRDD, colouring, ctx)

    result
  }

}