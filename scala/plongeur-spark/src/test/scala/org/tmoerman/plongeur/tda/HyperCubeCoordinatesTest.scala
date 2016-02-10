package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Skeleton.hyperCubeCoordinateVectors

/**
  * @author Thomas Moerman
  */
class HyperCubeCoordinatesTest extends FlatSpec with Matchers {

  behavior of "combining coordinates"

  it should "be correct with 1x1 interval coordinates" in {
    hyperCubeCoordinateVectors(Seq(Seq(1), Seq("a"))) shouldBe Set(

      Vector(1, "a"))
  }

  it should "be correct with 1x2 interval coordinates" in {
    hyperCubeCoordinateVectors(Seq(Seq(1), Seq("a", "b"))) shouldBe Set(

      Vector(1, "a"), Vector(1, "b"))
  }

  it should "be correct with 2x1 interval coordinates" in {
    hyperCubeCoordinateVectors(Seq(Seq(1, 2), Seq("a"))) shouldBe Set(

      Vector(1, "a"), Vector(2, "a"))
  }

  it should "be correct with 2x2 interval coordinates" in {
    hyperCubeCoordinateVectors(Seq(Seq(1, 2), Seq("a", "b"))) shouldBe Set(

      Vector(1, "a"), Vector(1, "b"),
      Vector(2, "a"), Vector(2, "b"))
  }

  it should "be correct with 3x3 interval coordinates" in {
    hyperCubeCoordinateVectors(Seq(Seq(1, 2, 3), Seq("a", "b", "c"))) shouldBe Set(

      Vector(1, "a"), Vector(1, "b"), Vector(1, "c"),
      Vector(2, "a"), Vector(2, "b"), Vector(2, "c"),
      Vector(3, "a"), Vector(3, "b"), Vector(3, "c"))
  }

  it should "be correct with 2x2x2 interval coordinates" in {
    hyperCubeCoordinateVectors(Seq(Seq(1, 2), Seq("a", "b"), Seq('X, 'Y))) shouldBe Set(

      Vector(1, "a", 'X), Vector(1, "b", 'X),
      Vector(2, "a", 'X), Vector(2, "b", 'X),

      Vector(1, "a", 'Y), Vector(1, "b", 'Y),
      Vector(2, "a", 'Y), Vector(2, "b", 'Y))
  }

  behavior of "integrating intersecting intervals and coordinate combination"

  it should "be correct in a 2x2 case" in {

  }

}
