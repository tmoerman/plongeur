package org.tmoerman.plongeur.util

import org.scalatest.{Matchers, FlatSpec}
import VectorFunctions._

/**
  * @author Thomas Moerman
  */
class VectorFunctionsSpec extends FlatSpec with Matchers {

  behavior of "Dropping by index"

  val v = Vector(1, 2, 3, 4, 5)

  it should "yield the same vector if the index is out of range" in {
    v.dropByIndex(-1) shouldBe v
    v.dropByIndex(v.length) shouldBe v
  }

  it should "work correctly at boundary indices" in {
    v.dropByIndex(0) shouldBe v.slice(1, v.length)
    v.dropByIndex(v.length - 1) shouldBe v.slice(0, v.length - 1)
  }

  it should "work correctly in middle indices" in {
    v.dropByIndex(2) shouldBe Vector(1, 2, 4, 5)
  }

}
