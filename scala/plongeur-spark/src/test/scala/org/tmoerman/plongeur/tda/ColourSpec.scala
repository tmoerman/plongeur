package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Colour.toBin

/**
  * @author Thomas Moerman
  */
class ColourSpec extends FlatSpec with Matchers {

  behavior of "pctToBin"

  it should "correctly calculate the bin for percentages" in {
    toBin(7)(0d)    shouldBe 0
    toBin(7)(0.55d) shouldBe 3
    toBin(7)((1d / 7)*6 + 0.1) shouldBe 6
  }

  behavior of "Brewer"

  import Colour._

  it should "correctly parse the palettes.json" in {

    // val cat0 = new AttributeEquals("cat", "0")

    // Colouring(Brewer.palettes("Blues").get(9), PercentageInCluster(9, cat0))
  }

}