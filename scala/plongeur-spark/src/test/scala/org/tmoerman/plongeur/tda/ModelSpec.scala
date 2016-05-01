package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class ModelSpec extends FlatSpec with Matchers {

  "a FilterFunction" should "not be instantiable with an illegal overlap value" in {
    intercept[IllegalArgumentException] {
      Filter(HNil, 100, -100)
    }

    intercept[IllegalArgumentException] {
      Filter(HNil, 100, 0.8)
    }
  }

}
