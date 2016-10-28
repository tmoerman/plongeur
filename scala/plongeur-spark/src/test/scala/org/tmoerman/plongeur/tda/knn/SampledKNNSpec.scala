package org.tmoerman.plongeur.tda.TestCommons

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.Commons._
import org.tmoerman.plongeur.tda.knn.SampledKNN.SampledKNNParams
import org.tmoerman.plongeur.tda.knn._
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class SampledKNNSpec extends FlatSpec with SparkContextSpec with Matchers {

  val kNNParams = SampledKNNParams(k = 2, sampleSize = Left(3), distance = EuclideanDistance)

  lazy val rdd = sc.parallelize(points)
  lazy val ctx = TDAContext(sc, rdd)

  "SampledKNN with fixed sample size" should "yield correct frequencies" in {
    val acc = SampledKNN(ctx, kNNParams)

    assertDistanceFrequenciesRDD(acc, Map(1.0 -> 6))
  }

}