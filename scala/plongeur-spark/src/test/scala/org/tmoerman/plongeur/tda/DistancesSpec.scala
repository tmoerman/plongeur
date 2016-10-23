package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors._
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
class DistancesSpec extends FlatSpec with Matchers {

  "Euclidean distance" should "work" in {
    val dist = EuclideanDistance.apply(
      dp(0, dense(0.0, 0.0)),
      dp(1, dense(0.0, 2.0)))

    dist shouldBe 2.0
  }

}