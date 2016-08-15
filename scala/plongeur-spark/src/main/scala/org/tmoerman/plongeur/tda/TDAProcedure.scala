package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
object TDAProcedure extends TDA {

  def apply(tdaParams: TDAParams, ctx: TDAContext): TDAResult = {
    import tdaParams._

    val amendedCtx = tdaParams.lens.amend(ctx)

    val levelSetClustersRDD = clusterLevelSets(lens, amendedCtx, clusteringParams)

    val partitionedClustersRDD = applyScale(levelSetClustersRDD, scaleSelection)

    val (clustersRDD, edgesRDD) = formClusters(partitionedClustersRDD, collapseDuplicateClusters)

    val result = applyColouring(clustersRDD, edgesRDD, colouring, amendedCtx)

    result
  }

}