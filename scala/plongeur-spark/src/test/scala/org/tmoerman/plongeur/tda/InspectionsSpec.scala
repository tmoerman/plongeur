package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}

/**
  * @author Thomas Moerman
  */
class InspectionsSpec extends FlatSpec with Matchers {

  val pretty = new MapToInt

  "MapToInt" should "return correct translations" in {

    pretty("a") shouldBe 0
    pretty("b") shouldBe 1
    pretty("c") shouldBe 2
    pretty("a") shouldBe 0
    pretty("b") shouldBe 1
    pretty("c") shouldBe 2

  }

}
