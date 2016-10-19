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

  implicit val ORD = Ordering.by[PQEntry, Distance](_._2).reverse

  /**
    * @return Returns a SparseMatrix in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: ACC) =
    SparseMatrix.fromCOO(N, N, for { (p, bpq) <- acc; (q, dist) <- bpq } yield (p.index, q, dist))

}