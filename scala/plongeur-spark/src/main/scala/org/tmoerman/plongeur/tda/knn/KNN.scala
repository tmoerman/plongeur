package org.tmoerman.plongeur.tda.knn

import org.apache.spark.mllib.linalg.SparseMatrix
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.tda.knn.SampledKNN.SampledKNNParams
import org.tmoerman.plongeur.util.BoundedPriorityQueue

/**
  * @author Thomas Moerman
  */
object KNN extends Serializable {

  type PQEntry = (Index, Distance)
  type BPQ     = BoundedPriorityQueue[PQEntry]
  type ACC     = List[(DataPoint, BPQ)]
  type ACCLike = Iterable[(DataPoint, BPQ)]

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: ACC) =
    SparseMatrix.fromCOO(N, N, for { (p, bpq) <- acc; (q, dist) <- bpq } yield (p.index, q, dist))

  /**
    * @param ctx TDAContext
    * @param candidate Candidate accumulator of which to assess the accuracy.
    * @param kNNParams Params for exact kNN calculation.
    * @return Returns the accuracy of specified accumulator acc with respect to the truth accumulator
    *         (calculated with a brute-force approach).
    */
  def accuracy(ctx: TDAContext, candidate: ACCLike, kNNParams: ExactKNNParams): Double = {
    val groundTruth = ExactKNN.toACC(ctx, kNNParams)

    (candidate, groundTruth) // TODO careful with sort orders!
      .zipped
      .map{ case ((c, c_NN), (t, t_NN)) =>
        val common = c_NN.map(_._1).toSet intersect t_NN.map(_._1).toSet
        common.size.toDouble / c_NN.size }
      .sum / candidate.size
  }

  /**
    * @param ctx TDAContext
    * @param candidate Candidate accumulator of which to assess the accuracy.
    * @param kNNParams Params for sampled kNN calculation.
    * @return Returns
    */
  def sampledAccuracy(ctx: TDAContext, candidate: ACCLike, kNNParams: SampledKNNParams): Double = {
    val sampledGroundTruth =
      SampledKNN
        .toACC(ctx, kNNParams)
        .map{ case (p, bpq) => (p.index, bpq.map(_._1).toSet) }
        .toMap

    val sampledIndexSet = sampledGroundTruth.keySet

    candidate
      .filter{ case (p, _) => sampledIndexSet.contains(p.index) }




    ???
  }

//  /**
//    * @param candidate
//    * @param groundTruth
//    * @param k
//    * @return
//    */
//  def accuracy(candidate: SparseMatrix, groundTruth: SparseMatrix)(implicit k: Int): Double = {
//    (candidate.rowVectors.toSeq, groundTruth.rowVectors.toSeq)
//      .zipped
//
//    ???
//  }

}