package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.cluster.Clustering.ScaleSelection
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
object Scale extends Serializable {

  def histogram(nrBins: Int = 10) = HistogramScaleSelection(nrBins)

  def danifold(nrBins: Int = 10) = DanifoldScaleSelection(nrBins)

  def firstGap(gapSizePct: Int = 10) = FirstGapScaleSelection(gapSizePct)

  def biggestGap() = BiggestGapScaleSelection

  /**
    * @param nrBins The number of histogram bins
    */
  case class HistogramScaleSelection(val nrBins: Int) extends ScaleSelection with Serializable {

    override def apply(heights: Seq[Double]): Double = heights.toList match {
      case Nil => 0
      case x :: Nil => x
      case _ =>
        val min = heights.min
        val max = heights.max
        val inc = (max - min) / nrBins

        def bin(d: Double) = try {
          (BigDecimal(d - min) quot inc).toInt
        } catch {
          case e: Exception => throw new Exception(s"d=$d, inc=$inc, min=$min, max=$max", e)
        }

        val frequencies = heights.map(bin).frequencies

        Stream
          .from(0)
          .takeWhile(_ < nrBins)
          .dropWhile(i => frequencies(i) != 0)
          .headOption
          .map(_ * inc + min) // TODO is this correct ???
          .getOrElse(max)
    }

    override val resolution = nrBins

    override def toString = s"Scale.histogram($nrBins)"

  }

  /**
    * See Danifold cutoff.py - histogram
    *
    * @param nrBins The number of histogram bins.
    */
  case class DanifoldScaleSelection(val nrBins: Int) extends ScaleSelection with Serializable {

    override def apply(heights: Seq[Double]): Double = heights.toList match {
      case Nil => 0
      case x :: Nil => x
      case _ =>
        val max = heights.max
        val inc = max / nrBins

        def bin(d: Double) = (BigDecimal(d) quot inc).toInt

        val frequencies = heights.map(bin).frequencies

        Stream
          .from(0)
          .takeWhile(_ < nrBins)
          .dropWhile(i => frequencies(i) != 0)
          .headOption
          .map(result => (result + 1) * inc)
          .getOrElse(max)
    }

    override val resolution = nrBins

    override def toString = s"Scale.danifold.histogram($nrBins)"

  }

  /**
    * @param gapPct The percentual size of the gap.
    */
  case class FirstGapScaleSelection(val gapPct: Int) extends ScaleSelection with Serializable {

    override def apply(heights: Seq[Double]): Double = heights.toList match {
      case Nil => 0
      case x :: Nil => x
      case _ =>
        lazy val max = heights.max

        heights
          .slidingPairs
          .dropWhile{ case (a, b) => (b - a) / max <= gapPct.toDouble / 100 }
          .headOption
          .map{ case (a, b) => (a + b) / 2 }
          .getOrElse(max)
    }

    override def resolution: Int = gapPct

    override def toString = s"Scale.firstGap($gapPct%)"

  }

  case object BiggestGapScaleSelection extends ScaleSelection with Serializable {

    override def apply(heights: Seq[Double]): Double = heights.toList match {
      case Nil => 0
      case x :: Nil => x
      case _ =>
        lazy val max = heights.max

        val (a, b) =
          heights
            .slidingPairs
            .map{ case t@(a, b) => (t, (b - a)) }
            .maxBy(_._2)
            ._1

        (a + b) / 2
    }

    override def resolution: Int = 0

    override def toString = s"Scale.biggestGap"

  }

}