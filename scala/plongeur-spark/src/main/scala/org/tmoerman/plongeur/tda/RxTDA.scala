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

  val groupDataPointsByLevelSets = (lens: TDALens, ctx: TDAContext) => {
    import ctx._

    val filterFunctions = lens.filters.map(f => toFilterFunction(f.spec, ctx))

    val boundaries = calculateBoundaries(filterFunctions, dataPoints)

    val levelSetsInverse = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    dataPoints
      .flatMap(dataPoint => levelSetsInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
      .groupByKey
      .cache
  }

  val clusterLevelSets = (clusterer: LocalClusteringProvider) => (byLevelSet: RDD[(LevelSetID, Iterable[DataPoint])],
                                                                  clusteringParams: ClusteringParams) => {
    import clusteringParams._

    byLevelSet
      .map { case (levelSetID, levelSetPoints) =>
        (levelSetID, levelSetPoints.toList, clusterer.apply(levelSetPoints.toSeq, distanceFunction, clusteringMethod)) }
      .cache
  }

  val selectClusteringScale = (tripleRDD: RDD[(LevelSetID, List[DataPoint], LocalClustering)],
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

  def tdaMachine(tdaParams$: Observable[TDAParams], ctx$: Observable[TDAContext]): Observable[TDAResult] = {

    val clusterer = SmileClusteringProvider // TODO inject


    // deconstructing the parameters

    val lens$                   = tdaParams$.map(_.lens).distinct

    val clusteringParams$       = tdaParams$.map(_.clusteringParams).distinct

    val scaleSelection$         = tdaParams$.map(_.scaleSelection).distinct

    val collapseDuplicates$     = tdaParams$.map(_.collapseDuplicateClusters).distinct

    // TDA computation merges in parameter changes

    val lensCtx$                = lens$.combineLatest(ctx$).map{ case (lens, ctx) => (lens, lens.assocFilterMemos(ctx)) }

    val byLevelSetRDD$          = lensCtx$.map(groupDataPointsByLevelSets.tupled)

    val tripleRDD$              = byLevelSetRDD$.combineLatest(clusteringParams$).map(clusterLevelSets(clusterer).tupled)

    val partitionedClustersRDD$ = tripleRDD$.combineLatest(scaleSelection$).map(selectClusteringScale.tupled)

    val tdaResult$              = partitionedClustersRDD$.combineLatest(collapseDuplicates$).map(makeTDAResult.tupled)

    tdaResult$
  }

}
