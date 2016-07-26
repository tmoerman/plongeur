package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.cluster.{BroadcastSmileClusteringProvider, SimpleSmileClusteringProvider}
import rx.lang.scala.Observable
import Model._

/**
  * @author Thomas Moerman
  */
object TDAMachine extends TDA {

  def run(ctx: TDAContext, tdaParams$: Observable[TDAParams]): Observable[(TDAParams, TDAResult)] = {

    // source observable with backpressure

    ctx.sc.broadcast()

    val tdaParamsSource$ = tdaParams$.distinctUntilChanged

    // deconstructing the parameters

    val lens$               = tdaParamsSource$.map(_.lens                     ).distinctUntilChanged
    val clusteringParams$   = tdaParamsSource$.map(_.clusteringParams         ).distinctUntilChanged
    val scaleSelection$     = tdaParamsSource$.map(_.scaleSelection           ).distinctUntilChanged
    val collapseDuplicates$ = tdaParamsSource$.map(_.collapseDuplicateClusters).distinctUntilChanged

    // TDA computation merges in parameter changes

    val ctx$                 = tdaParamsSource$.scan(ctx){ (ctx, params) => params.amend(ctx) }.distinctUntilChanged

    val lensCtx$             = lens$.combineLatest(ctx$)

    val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets.tupled)

    val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale.tupled)

    val paramsWithResult$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult.tupled)

    paramsWithResult$
  }

}