package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Colour.pctToBin

/**
  * @author Thomas Moerman
  */
class ColoursSpec extends FlatSpec with Matchers {

  behavior of "pctToBin"

  it should "correctly calculate the bin for percentages" in {
    pctToBin(7, 0d)    shouldBe 0
    pctToBin(7, 0.55d) shouldBe 3
    pctToBin(7, (1d / 7)*6 + 0.1) shouldBe 6
  }

  behavior of "Brewer"

  it should "correctly parse the palettes.json" in {

  }

}