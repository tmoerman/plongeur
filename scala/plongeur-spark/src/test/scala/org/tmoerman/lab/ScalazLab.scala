package org.tmoerman.lab

import org.scalatest.{FlatSpec, Matchers}

import scalaz.Scalaz._
import scalaz._

/**
  * @author Thomas Moerman
  */
class ScalazLab extends FlatSpec with Matchers {

  behavior of "merging maps"

  it should "work for heterogeneous maps" in {

    val r = Map("a" -> 1) |+| Map("b" -> 2)

    r shouldBe Map("a" -> 1, "b" -> 2)

  }

}