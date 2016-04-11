package org.tmoerman.plongeur.scratch

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.scratch.Kruskal._

/**
  * @author Thomas Moerman
  */
class KruskalSpec extends FlatSpec with Matchers {

  "Kruskal" should "blah" in {

    val result =
      kruskal(List(Edge('A,'B,7), Edge('A,'D,5),Edge('B,'C,8), Edge('B,'D,9),
                   Edge('B,'E,7), Edge('C,'E,5), Edge('D,'E,15), Edge('D,'F,6),
                   Edge('E,'F,8), Edge('E,'G, 9), Edge('F,'G,11)))

    result shouldBe List(Edge('E,'G,9.0), Edge('B,'E,7.0), Edge('A,'B,7.0), Edge('D,'F,6.0), Edge('C,'E,5.0), Edge('A,'D,5.0))

  }

}
