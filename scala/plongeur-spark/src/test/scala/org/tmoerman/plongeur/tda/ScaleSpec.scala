package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.cluster.Scale.histogram
import org.tmoerman.plongeur.test.{FileResources, TestResources}

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



}