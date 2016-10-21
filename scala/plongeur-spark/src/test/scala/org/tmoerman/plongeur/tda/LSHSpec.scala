package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors._
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class LSHSpec extends FlatSpec with SparkContextSpec with Matchers {

  val points = Seq(
    dp(0, dense(-2.0, 0.0,  7.0)),
    dp(1, dense( 1.0, 2.0,  4.0)),
    dp(2, dense(-1.0, 3.0,  6.0)),
    dp(3, dense( 8.0, 10.0, -3.0)))

  "maxRadius" should "calculate the maximum radius" in {
    LSH.maxRadius(TDAContext(sc, sc.parallelize(points))) shouldBe 10
  }

}