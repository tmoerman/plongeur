package org.tmoerman.plongeur.tda

import breeze.linalg.functions._
import org.apache.spark.mllib.linalg.VectorConversions._
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
object Distance {

  type DistanceMatrix = Array[Array[Double]]

  /**
    * See smile-scala.
    *
    * @param dataPoints The data points.
    * @param distanceFunction The distance function, e.g. Euclidean.
    * @return Returns a distance matrix
    */
  def distanceMatrix(dataPoints: Seq[DataPoint],
                     distanceFunction: DistanceFunction): DistanceMatrix = {
    val n = dataPoints.length
    val result = new Array[Array[Double]](n)

    for (i <- 0 until n) {
      result(i) = new Array[Double](i+1)
      for (j <- 0 until i) {
        result(i)(j) = distanceFunction(dataPoints(i), dataPoints(j))
      }
    }

    result
  }

  trait DistanceFunction extends ((DataPoint, DataPoint) => Double) with Serializable

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

  case class MinkowskiDistance(exponent: Double) extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = minkowskiDistance(a.features.toBreeze, b.features.toBreeze, exponent)
  }

  /**
    * @param name
    * @return Returns the DistanceFunction associated with specified name.
    */
  def from(name: String): (Any) => DistanceFunction = name match {
    case "chebyshev"  => (_)      => ChebyshevDistance
    case "cosine"     => (_)      => CosineDistance
    case "euclidean"  => (_)      => EuclideanDistance
    case "manhattan"  => (_)      => ManhattanDistance
    case "minkowski"  => (e: Any) => MinkowskiDistance(e.asInstanceOf[Double])

    case _ => throw new IllegalArgumentException(s"unknown distance function $name")
   }

}
