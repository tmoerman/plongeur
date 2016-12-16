package org.tmoerman.plongeur.tda.TestCommons

import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.Commons._
import org.tmoerman.plongeur.tda.knn.ExactKNN._

/**
  * @author Thomas Moerman
  */
class ExactKNNSpec extends FlatSpec with SharedSparkContext with Matchers {

  val params = ExactKNNParams(k = 2, distance = EuclideanDistance)

  lazy val rdd = sc.parallelize(points)
  lazy val ctx = TDAContext(sc, rdd)

  "ExactKNN" should "yield correct frequencies" in {
    val rdd = apply(ctx, params)

    assertDistanceFrequenciesRDD(rdd)
  }

}