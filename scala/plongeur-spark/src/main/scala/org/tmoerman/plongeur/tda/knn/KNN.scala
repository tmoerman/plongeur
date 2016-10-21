package org.tmoerman.plongeur.tda.knn

import org.apache.spark.mllib.linalg.SparseMatrix
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.BoundedPriorityQueue

/**
  * @author Thomas Moerman
  */
object KNN extends Serializable {

  type PQEntry = (Index, Distance)
  type BPQ     = BoundedPriorityQueue[PQEntry]
  type ACC     = List[(DataPoint, BPQ)]
  type ACCLike = Iterable[(DataPoint, BPQ)]

  val ORD = Ordering.by[PQEntry, Distance](_._2).reverse

  def bpq(k: Int) = new BPQ(k)(ORD)

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: ACC) =
    SparseMatrix.fromCOO(N, N, for { (p, bpq) <- acc; (q, dist) <- bpq } yield (p.index, q, dist))

  /**
    * @param candidate Candidate accumulator of which to assess the accuracy.
    * @param baseLine Ground truth accumulator to which the candidate will be compared.
    * @return Returns the accuracy of the candidate with respect to the baseline accumulator.
    */
  def accuracy(candidate: ACCLike, baseLine: ACCLike): Double = {
    val baseLineMap = baseLine.map{ case (p, bpq) => (p.index, bpq.map(_._1).toSet) }.toMap

    val sum =
      candidate
        .filter{ case (p, _) => baseLineMap.contains(p.index) }
        .map{ case (p, bpq) => (bpq.map(_._1).toSet intersect baseLineMap(p.index)).size.toDouble / bpq.size }.sum

    sum / baseLine.size
  }

  import org.tmoerman.plongeur.util.MatrixFunctions._

  /**
    * @param candidate Candidate SparseMatrix of which to assess the accuracy.
    * @param baseLine Ground truth SparseMatrix to which the candidate will be compared.
    * @return Returns the accuracy of the candidate with respect to the baseline SparseMatrix.
    */
  def accuracy(candidate: SparseMatrix, baseLine: SparseMatrix): Double = {
    (candidate.rowVectors.toSeq, baseLine.rowVectors.toSeq)
      .zipped

    ???
  }

}