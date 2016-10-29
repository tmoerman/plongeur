package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH._
import org.tmoerman.plongeur.tda.Model.{DataPoint, Index, TDAContext}
import org.tmoerman.plongeur.tda.knn.FastKNN_BAK.FastKNNParams

/**
  * @author Thomas Moerman
  */
object SymmetricFastKNN {

  type SymmetricKNN_RDD = RDD[((Index, Index), BPQ)]

  def apply(ctx: TDAContext, kNNParams: FastKNNParams): SymmetricKNN_RDD = {
    import kNNParams._
    import lshParams._

    val bc = ctx.sc.broadcast(FastKNN_ALT.hashProjectionFunctions(ctx, kNNParams, seed))



    val indexBound = ctx.indexBound



    ???
  }

}