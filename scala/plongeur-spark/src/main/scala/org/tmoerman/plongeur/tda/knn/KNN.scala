package org.tmoerman.plongeur.tda.knn

import breeze.linalg.{CSCMatrix => BSM}
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

  implicit val ORD = Ordering.by[PQEntry, Distance](_._2).reverse

  /**
    * @return Returns a Breeze sparse matrix (BSM) in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: ACCLike): BSM[Distance] = {
    val kNN = acc.map{ case (_, bpq) => bpq.toSeq.sortBy(_._1) }

    val distances   = kNN.flatMap(_.map(_._2)).toArray
    val rowIndices  = kNN.map(_.size).toArray
    val colPointers = kNN.flatMap(_.map(_._1)).toArray

    new BSM[Distance](distances, N, N, colPointers, rowIndices)
  }

}