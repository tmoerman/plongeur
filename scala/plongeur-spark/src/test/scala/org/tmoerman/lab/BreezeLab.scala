package org.tmoerman.lab

import breeze.linalg.SparseVector
import org.scalatest.{Matchers, FlatSpec}

/**
  * @author Thomas Moerman
  */
class BreezeLab extends FlatSpec with Matchers {

  behavior of "breeze vectors"

  it should "be possible to add disjunct vectors" in {

    val a = SparseVector(20)(0 -> 3.0, 10 -> 3.0)
    val b = SparseVector(20)(1 -> 7.0, 11 -> 7.0)

    val c = a + b

    c shouldBe SparseVector(20)(0 -> 3.0, 10 -> 3.0, 1 -> 7.0, 11 -> 7.0)
  }

  it should "be possible to add vectors with overlapping indices" in {

    val a = SparseVector(20)(0 -> 3.0, 10 -> 3.0)
    val b = SparseVector(20)(0 -> 7.0, 10 -> 7.0)

    val c = a + b

    c shouldBe SparseVector(20)(0 -> 10.0, 10 -> 10.0)
  }

  it should "be possible to compute the pairwise max of two vectors" in {

    val a = SparseVector(20)(0 -> 3.0, 10 -> 3.0)
    val b = SparseVector(20)(0 -> 7.0, 11 -> 7.0)

    val c = breeze.linalg.max(a, b)

    c shouldBe SparseVector(20)(0 -> 7.0, 10 -> 3.0, 11 -> 7.0)
  }

  it should "apply division by a scalar to all elements" in {

    val a = SparseVector(20)(0 -> 3.0, 10 -> 3.0)

    val c: SparseVector[Double] = (a / 2.0)

    c shouldBe SparseVector(20)(0 -> 1.5, 10 -> 1.5)
  }

  it should "apply exponentiation by a scalar to all elements" in {

    val a = SparseVector(20)(0 -> 3.0, 10 -> 3.0)

    val c: SparseVector[Double] = (a :^ 2.0)

    c shouldBe SparseVector(20)(0 -> 9.0, 10 -> 9.0)
  }

}