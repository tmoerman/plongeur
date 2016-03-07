package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Covering.uniformCoveringIntervals

/**
  * @author Thomas Moerman
  */
class CoveringIntervalsSpec extends FlatSpec with Matchers {

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
    val f = uniformCoveringIntervals(12, 22, .2, 0) _

    f(12) shouldBe bdSeq(12)

    f(12.1) shouldBe bdSeq(12)

    f(13.9) shouldBe bdSeq(12)

    f(17) shouldBe bdSeq(16)

    f(22) shouldBe bdSeq(22)
  }

  it should "be correct with 50% overlap" in {
    val f = uniformCoveringIntervals(12, 22, .2, 0.5) _

    f(12) shouldBe bdSeq(11, 12)

    f(13) shouldBe bdSeq(12, 13)

    f(13.5) shouldBe bdSeq(12, 13)

    f(14.5) shouldBe bdSeq(13, 14)

    f(22) shouldBe bdSeq(21, 22)
  }

  it should "be correct with 70% overlap" in {
    val f = uniformCoveringIntervals(12, 22, .2, .7) _

    f(12) shouldBe bdSeq(10.2, 10.8, 11.4, 12.0)

    f(16.9) shouldBe bdSeq(15.0, 15.6, 16.2, 16.8)

    f(22) shouldBe bdSeq(19.8, 20.4, 21, 21.6)
  }

}