package org.tmoerman.plongeur.tda

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Colour.Colouring
import org.tmoerman.plongeur.tda.Filters.extractBroadcasts
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering.{ClusteringParams, LocalClustering, ScaleSelection}
import rx.lang.scala.Observable
import rx.lang.scala.Observable._
import shapeless.{::, HList, HNil}

/**
  * @author Thomas Moerman
  */
object TDAMachine extends TDA {

  def run(tdaContext: TDAContext,
          tdaParams$: Observable[TDAParams]): Observable[(TDAParams, TDAResult)] = {

    val init: (TDAContext, Option[TDAParams]) = (tdaContext, None)

    tdaParams$
      .distinctUntilChanged
      .scan(init){ case ((ctx, _), params) => (params.amend(ctx), Some(params)) }
      .flatMapIterable{ case (ctx, opt) => opt.map(params => (params, ctx)) }
      .groupBy{ case (params, ctx) => params.lens}
      .flatMap{ case (lens, paramsCtx$) =>

        val cached$ = paramsCtx$.cache

        val params$ = cached$.map(_._1)
        val ctx$    = cached$.map(_._2)

        val lensCtx$ = just(lens).combineLatest(ctx$)

        val clusteringParams$    = params$.map(_.clusteringParams         ).distinctUntilChanged
        val scaleSelection$      = params$.map(_.scaleSelection           ).distinctUntilChanged
        val collapseDuplicates$  = params$.map(_.collapseDuplicateClusters).distinctUntilChanged
        val colouring$           = params$.map(_.colouring                ).distinctUntilChanged

        val levelSetClustersRDD$ = lensCtx$.combineLatest(clusteringParams$).map(flattenTuple).map(clusterLevelSets_P.tupled)
        val localClustersRDD$    = levelSetClustersRDD$.combineLatest(scaleSelection$).map(applyScale_P.tupled)
        val clustersAndEdges$    = localClustersRDD$.combineLatest(collapseDuplicates$).map(formClusters_P.tupled)
        val paramsAndResult$     = clustersAndEdges$.combineLatest(colouring$).map(applyColouring_P.tupled)

        paramsAndResult$
      }
  }

  // TODO better naming of following functions

  val clusterLevelSets_P = (lens: TDALens, ctx: TDAContext, clusteringParams: ClusteringParams) => {
    val rdd: RDD[(LevelSetID, (List[DataPoint], LocalClustering))] = clusterLevelSets(lens, ctx, clusteringParams)

    (clusteringParams :: lens :: HNil, rdd, ctx)
  }

  val applyScale_P = (product: (HList, RDD[(LevelSetID, (List[DataPoint], LocalClustering))], TDAContext), scaleSelection: ScaleSelection) => {
    val (hlist, levelSetClustersRDD, ctx) = product

    val rdd = applyScale(levelSetClustersRDD, scaleSelection)

    (scaleSelection :: hlist, rdd, ctx)
  }

  val formClusters_P = (product: (HList, RDD[List[Cluster]], TDAContext), collapseDuplicateClusters: Boolean) => {
    val (hlist, partitionedClustersRDD, ctx) = product

    val (clustersRDD, edgesRDD) = formClusters(partitionedClustersRDD, collapseDuplicateClusters)

    (collapseDuplicateClusters :: hlist, (clustersRDD, edgesRDD), ctx)
  }

  val applyColouring_P = (product: (HList, (RDD[Cluster], RDD[ClusterEdge]), TDAContext), colouring: Colouring) => {
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

      case _ => throw new IllegalStateException("dafuq?")
    }

    (reconstructedParams, result)
  }

}