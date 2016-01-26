package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import Model._

/**
  * @author Thomas Moerman
  */
class ModelTest extends FlatSpec with Matchers {

  "a FilterFunction" should "not be instantiable with an illegal overlap value" in {
    intercept[IllegalArgumentException] {
      Filter((a: Any) => 4, 100, -100)
    }

    intercept[IllegalArgumentException] {
      Filter((a: Any) => 4, 100, 0.8)
    }
  }

}
