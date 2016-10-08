package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
object TDAProcedure extends TDA {

  def apply(params: TDAParams, ctx: TDAContext): TDAResult = {
    import params._

    val amendedCtx = params.amend(ctx)

    val levelSetsRDD = createLevelSets(lens, amendedCtx)

    val levelSetClustersRDD = clusterLevelSets(levelSetsRDD, clusteringParams)

    val partitionedClustersRDD = applyScale(levelSetClustersRDD, scaleSelection)

    val (clustersRDD, edgesRDD) = formClusters(partitionedClustersRDD, collapseDuplicateClusters)

    val result = applyColouring(clustersRDD, edgesRDD, colouring, amendedCtx)

    result
  }

}