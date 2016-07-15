package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.cluster.Clustering.ScaleSelection
import org.tmoerman.plongeur.util.IterableFunctions._
import shapeless.{HNil, HList}
import shapeless.HList.ListCompat._

/**
  * @author Thomas Moerman
  */
object Scale extends Serializable {

//  def parseScale(scaleSpec: HList): ScaleSelection = scaleSpec match {
//    case "histogram" #: (nrBins: Int) #: HNil => histogram(nrBins)
//    case "first_gap" #: HNil                  => firstGap()
//
//    case _                                    => histogram()
//  }

  def histogram(nrBins: Int = 10) = HistogramScaleSelection(nrBins)

  case class HistogramScaleSelection(val nrBins: Int) extends ScaleSelection with Serializable {

    override def apply(heights: Seq[Double]): Double = heights.toList match {
      case Nil => 0
      case x :: Nil => x
      case _ =>
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

    override def toString = s"Scale.histogram($nrBins)"

  }

  def firstGap() = ???

}