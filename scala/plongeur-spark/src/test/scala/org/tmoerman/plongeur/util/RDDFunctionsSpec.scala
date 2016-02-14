package org.tmoerman.plongeur.util

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.TestResources

import RDDFunctions._

/**
  * @author Thomas Moerman
  */
class RDDFunctionsSpec extends FlatSpec with TestResources with Matchers {

  behavior of "dropping lines of an RDD"

  it should "work" in {

    irisParsed._2.drop(1).first._1 shouldBe 4.9

    irisParsed._2.drop(3).first._1 shouldBe 4.6

  }

}