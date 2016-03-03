package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
object Scale extends Serializable {

  def histogram(nrBins: Int = 10) = (clustering: LocalClustering) => clustering.heights(true).toList match {
    case Nil      => 0
    case x :: Nil => 0
    case heights  =>
      val inc = (heights.max - heights.min) / nrBins

      val frequencies = heights.map(d => (BigDecimal(d) quot inc).toInt).frequencies

      val head =
        (frequencies.keys.min to frequencies.keys.max)
          .dropWhile(x => frequencies(x) != 0)
          .headOption

      head.getOrElse(1) * inc
  }

  def firstGap() = ???

}