package org.tmoerman.plongeur.util

import org.scalatest.{Matchers, FlatSpec}

import MapFunctions._

/**
  * @author Thomas Moerman
  */
class MapFunctionsSpec extends FlatSpec with Matchers {

  behavior of "merge"

  it should "merge a non-empty map with an empty map" in {
    val merged = Map("a" -> 1, "b" -> 2).merge(_ + _)(Map())

    merged shouldBe Map("a" -> 1, "b" -> 2)
  }

  it should "merge two non-empty maps with non-equal keys" in {
    val merged = Map("a" -> 1).merge(_ + _)(Map("b" -> 2))

    merged shouldBe Map("a" -> 1, "b" -> 2)
  }

  it should "merge two non-empty maps with equal keys" in {
    val merged = Map("a" -> 1, "b" -> 2).merge(_ + _)(Map("a" -> 10, "b" -> 20))

    merged shouldBe Map("a" -> 11, "b" -> 22)
  }

}