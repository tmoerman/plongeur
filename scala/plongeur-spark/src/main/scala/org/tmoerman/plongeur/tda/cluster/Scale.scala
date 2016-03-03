package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
object Scale extends Serializable {

  def histogram(nrBins: Int = 10) = (heights: Seq[Double]) => heights.toList match {
    case      Nil => 0
    case x :: Nil => x
    case        _ =>
      val min = heights.min
      val max = heights.max
      val inc = (max - min) / nrBins

      def bin(d: Double) = (BigDecimal(d - min) quot inc).toInt

      val frequencies = heights.map(bin).frequencies

      Stream
        .from(0)
        .dropWhile(i => frequencies(i) != 0)
        .map(_ * inc + min)
        .head
  }

  def firstGap() = ???

}