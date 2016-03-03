package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.cluster.Scale

/**
  * @author Thomas Moerman
  */
class ScaleSpec extends FlatSpec with Matchers {

  behavior of "histogram scale selection"

  val hist = Scale.histogram(10)

  it should "yield 0 if the heights are empty" in {
    hist.apply(Nil) shouldBe 0
  }

  it should "yield 0 if the heights are a singleton" in {
    hist.apply(66 :: Nil) shouldBe 66
  }

  it should "yield correctly 1" in {
    hist.apply(10.0 :: 22.0 :: Nil) shouldBe 11.2
  }

  it should "yield correctly 2" in {
    hist.apply(10.0 :: 11.2 :: 22.0 :: Nil) shouldBe 11.2
  }

}