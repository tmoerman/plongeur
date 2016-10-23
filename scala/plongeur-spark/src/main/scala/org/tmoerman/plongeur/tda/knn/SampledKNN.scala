package org.tmoerman.plongeur.tda.knn

import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.ExactKNN._
import org.tmoerman.plongeur.tda.knn.KNN._

import scala.util.Random.nextLong

/**
  * @author Thomas Moerman
  */
object SampledKNN {

  type Amount = Int
  type Fraction = Double

  case class SampledKNNParams(k: Int,
                              sampleSize: Either[Amount, Fraction] = Right(0.10),
                              distance: DistanceFunction = DEFAULT)
                             (implicit val seed: Long = nextLong)

  /**
    * @param ctx
    * @param kNNParams
    * @return Returns a partial sampleSize*N
    */
  def sampledACC(ctx: TDAContext, kNNParams: SampledKNNParams): ACC = {
    import kNNParams._

    implicit val k = kNNParams.k
    implicit val distance = kNNParams.distance

    val full = ctx.dataPoints

    lazy val taken =
      full
        .zipWithIndex
        .flatMap{ case (e, idx) => if (idx < sampleSize.left.get) e :: Nil else Nil }

    lazy val sampled =
      full
        .sample(withReplacement = false, fraction = sampleSize.right.get, seed = seed)

    val sample = if (sampleSize.isLeft) taken else sampled

    (sample cartesian full)
      .filter{ case (p, q) => p.index != q.index }
      .map{ case (p, q) =>
        val d = distance(p, q)

        (p, (q.index, d))
      }
      .combineByKey(init, concat, union)
      .collect
      .toList // TODO sort?
  }

}