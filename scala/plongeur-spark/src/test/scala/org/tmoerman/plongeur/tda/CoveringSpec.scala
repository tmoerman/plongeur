package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Covering.uniformCoveringIntervals

/**
  * @author Thomas Moerman
  */
class CoveringSpec extends FlatSpec with Matchers {

  def bdSeq(d: Double *) = d.map(BigDecimal(_))

  /*

    |----------|
  12.0
       |----------|
     12.6
          |----------|
        13.2
             |----------|
           13.8
                |----------|
              14.4
                   |---------+|
                [15.0]       |                <-.
                      |------+---|               \
                   [15.6]    |                <-- \
                         |---+------|              } these 4 are the intervals that match 16.9
                      [16.2] |                <-- /
                            |+---------|         /
                         [16.8]               <-'
                16.9 ------> x |----------|

                                  |----------|

  */

  behavior of "calculating intersecting intervals"

  it should "be correct with 0% overlap" in {
    val f = uniformCoveringIntervals(12, 22, 5, 0) _

    f(12) shouldBe bdSeq(12)

    f(12.1) shouldBe bdSeq(12)

    f(13.9) shouldBe bdSeq(12)

    f(17) shouldBe bdSeq(16)

    f(22) shouldBe bdSeq(22)
  }

  it should "be correct with 50% overlap" in {

    val f = uniformCoveringIntervals(0, 8, 3, 0.50) _

    f(0) shouldBe bdSeq(-2, 0)
    f(1) shouldBe bdSeq(-2, 0)

    f(2) shouldBe bdSeq(0, 2)
    f(3) shouldBe bdSeq(0, 2)

    f(4) shouldBe bdSeq(2, 4)
    f(5) shouldBe bdSeq(2, 4)

    f(6) shouldBe bdSeq(4, 6)
    f(7) shouldBe bdSeq(4, 6)

    f(8) shouldBe bdSeq(6, 8)
  }

  // TODO extend tests

}