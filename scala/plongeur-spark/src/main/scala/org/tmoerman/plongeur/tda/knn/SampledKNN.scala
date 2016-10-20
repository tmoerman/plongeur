package org.tmoerman.plongeur.tda.knn

import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.KNN._

import scala.util.Random.nextLong
import ExactKNN._

/**
  * @author Thomas Moerman
  */
object SampledKNN {

  case class SampledKNNParams(k: Int,
                              fraction: Double = 0.10,
                              distance: DistanceFunction = DEFAULT)
                             (implicit val seed: Long = nextLong)

  def toACC(ctx: TDAContext, kNNParams: SampledKNNParams): ACC = {
    import kNNParams._

    implicit val k = kNNParams.k
    implicit val distance = kNNParams.distance

    val full   = ctx.dataPoints.keyBy(_.index)
    val sample = full.sample(false, fraction)

    sample
      .join(full)
      .values
      .map{ case (s, f) =>
        val d = distance(s, f)

        (s, (f.index, d)) }
      .combineByKey(init, concat, union)
      .collect
      .toList
  }

}