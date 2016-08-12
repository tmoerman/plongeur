package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Colour.{Colouring, pctToBin}

/**
  * @author Thomas Moerman
  */
class ColourSpec extends FlatSpec with Matchers {

  behavior of "pctToBin"

  it should "correctly calculate the bin for percentages" in {
    pctToBin(7, 0d)    shouldBe 0
    pctToBin(7, 0.55d) shouldBe 3
    pctToBin(7, (1d / 7)*6 + 0.1) shouldBe 6
  }

  behavior of "Brewer"

  import Colour._

  it should "correctly parse the palettes.json" in {

    val cat0 = new AttributeSelector[Boolean]("cat") {
      override def apply(number: Any): Boolean = number.asInstanceOf[Int] == 0
    }

    //Colouring(Brewer.palettes("Blues").get(9), LocalPercentage(9, cat0, ),)
  }

}