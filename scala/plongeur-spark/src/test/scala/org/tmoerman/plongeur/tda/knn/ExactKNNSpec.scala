package org.tmoerman.plongeur.tda.knn

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.ExactKNN._
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class ExactKNNSpec extends FlatSpec with SparkContextSpec with Matchers {

  val params = ExactKNNParams(k = 2, distance = EuclideanDistance)

  lazy val rdd = sc.parallelize(points)
  lazy val ctx = TDAContext(sc, rdd)

  "ExactKNN ACC" should "yield correct frequencies" in {
    val rdd = apply(ctx, params)

    assertDistanceFrequenciesRDD(rdd)
  }

}