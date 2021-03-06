package org.tmoerman.plongeur.tda.knn

import org.tmoerman.plongeur.tda.Distances.{DEFAULT_DISTANCE, DistanceFunction}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.FastKNN.FastKNNParams
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * Brute force but exact k-NN implementation, mainly for A/B testing purposes.
  *
  * @author Thomas Moerman
  */
object ExactKNN {

  implicit def convert(p: FastKNNParams): ExactKNNParams = ExactKNNParams(p.k, p.lshParams.distance)

  case class ExactKNNParams(k: Int, distance: DistanceFunction = DEFAULT_DISTANCE)

  def apply(ctx: TDAContext, kNNParams: ExactKNNParams): KNN_RDD = {
    import kNNParams._

    implicit val k = kNNParams.k

    ctx
      .dataPoints
      .distinctComboPairs
      .flatMap{ case (a, b) =>
        val d = distance(a, b)

        (a, (b.index, d)) :: (b, (a.index, d)) :: Nil }
      .combineByKey(lift, add, union)
      .map{ case (p, bpq) => (p.index, bpq) }
  }

  def lift(entry: PQEntry)(implicit k: Int) = bpq(k) += entry

  def add(bpq: BPQ, entry: PQEntry) = bpq += entry

  def union(a: BPQ, b: BPQ) = a ++= b

}