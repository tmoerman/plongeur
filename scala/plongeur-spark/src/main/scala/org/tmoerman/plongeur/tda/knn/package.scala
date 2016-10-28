package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.SparseMatrix
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.BoundedPriorityQueue

/**
  * @author Thomas Moerman
  */
package object knn {

  type PQEntry     = (Index, Distance)
  type BPQ         = BoundedPriorityQueue[PQEntry]
  type Accumulator = List[(DataPoint, BPQ)]
  type kNN_RDD     = RDD[(Index, BPQ)]

  val ORD = Ordering.by((e: PQEntry) => (-e._2, e._1)) // why `e._1`? -> to disambiguate between equal distances
  def bpq(k: Int) = new BPQ(k)(ORD)

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: Accumulator) =
    SparseMatrix.fromCOO(N, N, for { (p, bpq) <- acc; (q, dist) <- bpq } yield (p.index, q, dist))

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, rdd: kNN_RDD) =
    SparseMatrix.fromCOO(N, N, rdd.flatMap{ case (p, bpq) => bpq.map{ case (q, dist) => (p, q, dist) }}.collect)

  /**
    * TODO
    *
    * implement an algorithm that collects:
    *   - true positives
    *   - false positives
    *   - false negatives
    *
    * so we can design some kind of learning algorithm that optimizes the LSH parameter choices for a data set.
    */

  /**
    * @param candidate Candidate accumulator of which to assess the accuracy.
    * @param baseLine Ground truth accumulator to which the candidate will be compared.
    * @return Returns the accuracy of the candidate with respect to the baseline accumulator.
    */
  def relativeAccuracy(candidate: kNN_RDD, baseLine: kNN_RDD): Double =
    (baseLine join candidate)
      .map{ case (_, (bpq1, bpq2)) => (bpq1.map(_._1).toSet intersect bpq2.map(_._1).toSet).size.toDouble / bpq1.size }
      .sum / baseLine.count

  import org.tmoerman.plongeur.util.MatrixFunctions._

  /**
    * @param candidate Candidate SparseMatrix of which to assess the accuracy.
    * @param baseLine Ground truth SparseMatrix to which the candidate will be compared.
    * @return Returns the accuracy of the candidate with respect to the baseline SparseMatrix.
    */
  def relativeAccuracy(candidate: SparseMatrix, baseLine: SparseMatrix): Double = {
    (candidate.rowVectors.toSeq, baseLine.rowVectors.toSeq)
      .zipped

    ???
  }

}