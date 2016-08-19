package org.tmoerman.plongeur.tda.cluster

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.cluster.Scale.histogram

/**
  * @author Thomas Moerman
  */
class ScaleSpec extends FlatSpec with Matchers {

  behavior of "histogram(10) scale selection"

  it should "yield 0 if the heights are empty" in {
    histogram(10).apply(Nil) shouldBe 0
  }

  it should "yield the singleton value for a singleton of heights" in {
    histogram(10).apply(66 :: Nil) shouldBe 66
  }

  it should "yield correctly for a pair of heights" in {
    val heights = Seq(10.0, 22.0)

    histogram(10).apply(heights) shouldBe 11.2
  }

  it should "yield correctly 2" in {
    val heights = Seq(10.0, 11.2, 22.0)

    histogram(10).apply(heights) shouldBe 11.2
  }

  behavior of "danifold(10) scale selection"

  it should "see Danifold mapper example 1" in {
    val heights = Seq(1, 1, 1, 2, 3, 4, 11).map(_.toDouble)

    Scale.danifold(10).apply(heights) shouldBe 5.5
  }

  it should "see Danifold mapper example 2" in {
    val heights = Seq(1, 1, 1, 2, 3, 5, 11).map(_.toDouble)

    Scale.danifold(10).apply(heights) shouldBe 4.4
  }

  behavior of "firstGap(20%)"

  it should "yield correct" in {
    val heights = Seq(1, 1, 1, 2, 3, 4, 11).map(_.toDouble)

    Scale.firstGap(20).apply(heights) shouldBe 7.5
  }

  behavior of "biggestGap"

  it should "yield correct" in {
    val heights = Seq(1, 1, 1, 2, 3, 4, 11).map(_.toDouble)

    Scale.biggestGap().apply(heights) shouldBe 7.5
  }

}