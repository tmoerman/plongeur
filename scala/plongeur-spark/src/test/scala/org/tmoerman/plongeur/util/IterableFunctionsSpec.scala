package org.tmoerman.plongeur.util

import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Thomas Moerman
  */
class IterableFunctionsSpec extends FlatSpec with Matchers {

  import IterableFunctions._

  behavior of "ordering on a collection of collections"

  it should "order a list of different vectors" in {
    List(Vector(1, 5), Vector(4, 9), Vector(3, 3), Vector(3, 2)).sorted shouldBe
    List(Vector(1, 5), Vector(3, 2), Vector(3, 3), Vector(4, 9))
  }

  it should "order a list of equal vectors" in {
    List(Vector(1, 2), Vector(1, 2), Vector(1, 2)).sorted shouldBe
    List(Vector(1, 2), Vector(1, 2), Vector(1, 2))
  }

  it should "order an empty list" in {
    List[BigDecimal]().sorted shouldBe Nil
  }

  it should "order a list of singleton vectors" in {
    List(Vector(3), Vector(0), Vector(2)).sorted shouldBe
    List(Vector(0), Vector(2), Vector(3))
  }

  it should "order a list of complex vectors" in {
    List(Seq(2, 4, 3, 7), Seq(2, 4, 0, 1), Seq(2, 4, 3, 1)).sorted shouldBe
    List(Seq(2, 4, 0, 1), Seq(2, 4, 3, 1), Seq(2, 4, 3, 7))
  }

  behavior of "frequencies"

  it should "be correct for empty input" in {
    Nil.frequencies shouldBe Map()
  }

  it should "be correct for singleton input" in {
    (666 :: Nil).frequencies shouldBe Map(666 -> 1)
  }

  it should "be correct for normal input" in {
    Seq(1, 2, 2, 3, 3, 3).frequencies shouldBe Map(1 -> 1, 2 -> 2, 3 -> 3)
  }

}
