package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Colour.toBin

/**
  * @author Thomas Moerman
  */
class ColourSpec extends FlatSpec with Matchers {

  behavior of "pctToBin"

  it should "correctly calculate the bin for percentages" in {
    toBin(10)(0.09d) shouldBe 0
    toBin(10)(0.11d) shouldBe 1
    toBin(10)(0.55d) shouldBe 5
    toBin(10)(0.99d) shouldBe 9
    toBin(10)(1.00d) shouldBe 9
  }

}