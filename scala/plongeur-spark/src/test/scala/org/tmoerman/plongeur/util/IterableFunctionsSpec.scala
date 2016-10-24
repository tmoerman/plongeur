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

  behavior of "cartesian"

  it should "yield nil when either or both of the iterables is nil" in {
    val empty: Seq[Int] = Nil

    (Seq(1) cartesian empty ) shouldBe Nil
    (empty  cartesian Seq(1)) shouldBe Nil
    (empty  cartesian empty ) shouldBe Nil
  }

  it should "yield correctly ordered pairs only" in {
    (Seq(1) cartesian Seq(2)) shouldBe Seq((1, 2))
    (Seq(2) cartesian Seq(1)) shouldBe Seq((1, 2))

    (Seq(2, 3, 4) cartesian Seq(1)).toSet shouldBe Set((1, 2), (1, 3), (1, 4))
  }

  behavior of "frequencies"

  it should "be correct for empty input" in {
    Nil.frequencies shouldBe Map.empty
  }

  it should "be correct for singleton input" in {
    (666 :: Nil).frequencies shouldBe Map(666 -> 1)
  }

  it should "be correct for normal input" in {
    Seq(1, 2, 2, 3, 3, 3).frequencies shouldBe Map(1 -> 1, 2 -> 2, 3 -> 3)
  }

  behavior of "groupWhile"

  it should "group an empty list correctly" in {
    val empty = List[Int]()

    empty.groupWhile(_ < _) shouldBe Nil
  }

  it should "group a singleton list correctly" in {
    val single = List(1)

    single.groupWhile(_ < _) shouldBe List(List(1))
  }

  it should "group a monotonously increasing list correctly" in {
    val mono = List(1, 2, 3, 4, 5)

    mono.groupWhile(_ < _) shouldBe List(List(1, 2, 3, 4, 5))
  }

  it should "group a monotonously decreasing list correctly" in {
    val rev = List(1, 2, 3, 4, 5).reverse

    rev.groupWhile(_ < _) shouldBe List(List(5), List(4), List(3), List(2), List(1))
  }

  it should "group a non-empty list correctly" in {
    val list = List(0, 1, 2, 0, 1, 4, 2, 7, 9, 1)

    list.groupWhile(_ < _) shouldBe List(List(0, 1, 2), List(0, 1, 4), List(2, 7, 9), List(1))
  }

}
