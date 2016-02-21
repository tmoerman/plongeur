package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.FileResources

/**
  * @author Thomas Moerman
  */
class InspectionsSpec extends FlatSpec with FileResources with Matchers {

  val cachedInt = Inspections.mapToInt

  "MapToInt" should "return correct translations" in {
    cachedInt("a") shouldBe 0
    cachedInt("b") shouldBe 1
    cachedInt("c") shouldBe 2
    cachedInt("a") shouldBe 0
    cachedInt("b") shouldBe 1
    cachedInt("c") shouldBe 2
  }

  "reading file" should "work" in {
    println(heuristicData.mkString("\n"))
  }

}
