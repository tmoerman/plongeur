package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.SparseMatrix
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.BoundedPriorityQueue
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
package object knn {

  type PQEntry     = (Index, Distance)
  type BPQ         = BoundedPriorityQueue[PQEntry]
  type Accumulator = List[(DataPoint, BPQ)]
  type KNN_RDD     = RDD[(Index, BPQ)]

  /**
    * @param k The k in kNN.
    * @param nrHashTables Also known as the L parameter, cfr. LSH literature.
    * @param nrJobs A technical parameter for breaking up the task in multiple jobs. Motivation is to have a strategy
    *               to limit the amount of memory needed for all table-related tuples in flight.
    * @param blockSize
    * @param lshParams
    */
  case class FastKNNParams(k: Int,
                           blockSize: Int,
                           nrHashTables: Int = 1,
                           nrJobs: Int = 1,
                           lshParams: LSHParams) {
    require(k > 0)
    require(nrHashTables > 0)
  }

  val ORD = Ordering.by((e: PQEntry) => (-e._2, e._1)) // why `e._1`? -> to disambiguate between equal distances
  def bpq(k: Int) = new BPQ(k)(ORD)

  /**
    * @return Returns a new accumulator.
    */
  def init(k: Int)(p: DataPoint): Accumulator = (p, bpq(k)) :: Nil

  /**
    * @return Returns an updated accumulator.
    */
  def concat(k: Int)(acc: Accumulator, p: DataPoint)(implicit distance: DistanceFunction): Accumulator = {
    val distances = acc.map{ case (q, bpq) => (q.index, distance(p, q)) }

    val newEntry = (p, distances.foldLeft(bpq(k)){ case (bpq, pair) => bpq += pair })

    val updated =
      (acc, distances)
        .zipped
        .map{ case ((q, bpq), (_, d)) => (q, bpq += ((p.index, d))) }

    newEntry :: updated
  }

  /**
    * Assumes accumulators a and b's data points are mutually exclusive.
    *
    * @return Returns a merged accumulator,
    *         cfr. G = U{g_1}, cfr. basic_ann_by_lsh(X, k, block-sz), p666 Y.-M. Zhang et al.
    */
  def merge(N: Int)(a: Accumulator, b: Accumulator)(implicit distance: DistanceFunction): Accumulator = {
    implicit val ORD = Ordering.by((d: DataPoint) => d.index)

    val distances =
      (a.map(_._1) cartesian b.map(_._1))
        .flatMap{ case (p, q) => {
          val d = distance(p, q)

          (p.index, q.index, d) :: (q.index, p.index, d) :: Nil
        }}

    val cache = SparseMatrix.fromCOO(N, N, distances)

    def merge(acc: Accumulator, arg: Accumulator): Accumulator =
      acc.map{ case (p, bpq) => (p, bpq ++= arg.map{ case (q, _) => (q.index, cache(p.index, q.index)) })}

    merge(a, b) ::: merge(b, a)
  }

  /**
    * @param a
    * @param b
    * @return Returns a combined KNN_RDD
    */
  def combine(a: KNN_RDD, b: KNN_RDD) = (a join b).mapValues[BPQ]{ case (bpq1, bpq2) => bpq1 ++= bpq2 }

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: Accumulator) =
    SparseMatrix.fromCOO(N, N, for { (p, bpq) <- acc; (q, dist) <- bpq } yield (p.index, q, dist))

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, rdd: KNN_RDD) =
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
  def relativeAccuracy(candidate: KNN_RDD, baseLine: KNN_RDD): Double =
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