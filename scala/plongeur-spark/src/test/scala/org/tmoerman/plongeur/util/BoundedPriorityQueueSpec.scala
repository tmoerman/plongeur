package org.tmoerman.plongeur.util

import org.scalatest.{Matchers, FlatSpec}

/**
  * @author Thomas Moerman
  */
class BoundedPriorityQueueSpec extends FlatSpec with Matchers {

  behavior of "BPQ"

  it should "be idempotent" in {
    val q = new BoundedPriorityQueue[Int](5)(Ordering.Int.reverse)

    q ++= Seq(0, 1, 2)
    q ++= Seq(0, 1, 2)

    q.toSet shouldBe Set(0, 1, 2)
  }

  it should "be commutative" in {
    val q1, q2, q3, q4 = new BoundedPriorityQueue[Int](5)(Ordering.Int.reverse)

    q1 ++= Seq(1, 3, 5, 7, 9, 11, 13)
    q2 ++= Seq(1, 3, 5, 7, 9, 11, 13)

    q3 ++= Seq(0, 2, 4, 6, 8, 10, 12)
    q4 ++= Seq(0, 2, 4, 6, 8, 10, 12)

    val q13 = (q1 ++= q3)
    val q42 = (q4 ++= q2)

    q13.toSet shouldBe q42.toSet
  }

}