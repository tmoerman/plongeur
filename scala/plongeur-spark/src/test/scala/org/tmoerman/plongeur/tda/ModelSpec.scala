package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class ModelSpec extends FlatSpec with Matchers {

  "initializing a DataPoint with meta data" should "allow Serializable values" in {
    IndexedDataPoint(
      index = 10,
      features = dense(1, 2, 3),
      meta = Some(Map("int" -> 1,
                      "str" -> "a")))
  }

  "a FilterFunction" should "not be instantiable with an illegal overlap value" in {
    intercept[IllegalArgumentException] {
      Filter(HNil, 100, -100)
    }

    intercept[IllegalArgumentException] {
      Filter(HNil, 100, 0.8)
    }
  }

}
