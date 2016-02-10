package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.{TestResources, SparkContextSpec}

/**
  * @author Thomas Moerman
  */
class SyntheticPointsSpec extends FlatSpec with TestResources with Matchers {

  behavior of "TODO"

  it should "bla" in {

    println(pointsRDD._2.collect().mkString("\n"))

  }

}
