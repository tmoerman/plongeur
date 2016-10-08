package org.tmoerman.plongeur.tda

import breeze.linalg.functions._
import org.apache.spark.mllib.linalg.VectorConversions._
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
object Distance {

  /**
    * See Smile-scala.
    *
    * @param dataPoints       The data points.
    * @param distanceFunction The distance function, e.g. Euclidean.
    * @return Returns a distance matrix, represented as an array of arrays of doubles.
    */
  def distanceMatrix(dataPoints: Seq[DataPoint], distanceFunction: DistanceFunction): Array[Array[Double]] = {
    val n = dataPoints.length
    val result = new Array[Array[Double]](n)

    for (i <- 0 until n) {
      result(i) = new Array[Double](i + 1)
      for (j <- 0 until i) {
        result(i)(j) = distanceFunction(dataPoints(i), dataPoints(j))
      }
    }

    result
  }

  val DEFAULT: DistanceFunction = ManhattanDistance

  trait DistanceFunction extends ((DataPoint, DataPoint) => Double) with SimpleName with Serializable

  // TODO Pearson correlation, closing over ~~TDAContext~~ / over broadcast variable

  // TODO Spearman

  case object ChebyshevDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = chebyshevDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case object CosineDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = cosineDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case object EuclideanDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = euclideanDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case object ManhattanDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = manhattanDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case class LpNormDistance(p: Double) extends NormBasedDistance with DistanceFunction {
    override protected def normConstant: Double = p

    override def apply(a: DataPoint, b: DataPoint) = apply(a.features.toBreeze, b.features.toBreeze)

    override def toString = { val name = getClass.getSimpleName; s"$name($p)" }
  }

  case class MinkowskiDistance(exponent: Double) extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = minkowskiDistance(a.features.toBreeze, b.features.toBreeze, exponent)

    override def toString = { val name = getClass.getSimpleName; s"$name($exponent)" }
  }

}