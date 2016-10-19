package org.tmoerman.plongeur.tda.knn

import breeze.linalg.{CSCMatrix => BSM}
import org.tmoerman.plongeur.tda.Distances.{DEFAULT, Distance, DistanceFunction}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.FastKNN.FastKNNParams
import org.tmoerman.plongeur.tda.knn.KNN._
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * Brute force but exact k-NN implementation, mainly for A/B testing purposes.
  *
  * @author Thomas Moerman
  */
object ExactKNN {

  implicit def convert(p: FastKNNParams): ExactKNNParams = ExactKNNParams(p.k, p.lshParams.distance)

  case class ExactKNNParams(k: Int, distance: DistanceFunction = DEFAULT)

  def apply(ctx: TDAContext, kNNParams: ExactKNNParams): BSM[Distance] = {
    val acc = toACC(ctx, kNNParams)

    toSparseMatrix(ctx.N, acc)
  }

  def toACC(ctx: TDAContext, kNNParams: ExactKNNParams): ACCLike = {
    implicit val k = kNNParams.k
    implicit val distance = kNNParams.distance

    ctx
      .dataPoints
      .distinctComboPairs
      .flatMap{ case (a, b) =>
        val d = distance(a, b)

        (a, (b.index, d)) ::
          (b, (a.index, d)) :: Nil }
      .combineByKey(init, concat, union)
      .collect
  }

  def init(entry: PQEntry)(implicit k: Int) = new BPQ(k) += entry

  def concat(bpq: BPQ, entry: PQEntry) = bpq += entry

  def union(a: BPQ, b: BPQ) = a ++= b

}