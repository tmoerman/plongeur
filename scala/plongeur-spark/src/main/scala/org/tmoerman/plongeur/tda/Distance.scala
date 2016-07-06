package org.tmoerman.plongeur.tda

import breeze.linalg.functions._
import org.apache.spark.mllib.linalg.VectorConversions._
import org.tmoerman.plongeur.tda.Model._
import shapeless._

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

  trait DistanceFunction extends ((DataPoint, DataPoint) => Double) with Serializable {
    override def toString = getClass.getSimpleName
  }

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
    override def toString = { val name = getClass.getSimpleName; s"$name($exponent)" }
  }

  /**
    * @param distanceSpec
    * @return Returns a DistanceFunction for specified spec.
    */
  def parseDistance(distanceSpec: HList): DistanceFunction = distanceSpec match {
    case "chebyshev" :: HNil             => ChebyshevDistance
    case "cosine"    :: HNil             => CosineDistance
    case "euclidean" :: HNil             => EuclideanDistance
    case "manhattan" :: HNil             => ManhattanDistance
    case "minkowski" :: (e: Any) :: HNil => MinkowskiDistance(e.asInstanceOf[Double])

    case _                               => EuclideanDistance
  }

}
