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

  type DistanceFunction = (DataPoint, DataPoint) => Double

  // TODO Pearson correlation, closing over ~~TDAContext~~ / over broadcast variable

  // TODO Spearman

  val L_infinity = (a: DataPoint, b: DataPoint) => chebyshevDistance(a.features.toBreeze, b.features.toBreeze)

  val cosine    = (a: DataPoint, b: DataPoint) => cosineDistance(a.features.toBreeze, b.features.toBreeze)

  val euclidean = (a: DataPoint, b: DataPoint) => euclideanDistance(a.features.toBreeze, b.features.toBreeze)

  val manhattan = (a: DataPoint, b: DataPoint) => manhattanDistance(a.features.toBreeze, b.features.toBreeze)

  def minkowski(exponent: Double) = (a: DataPoint, b: DataPoint) => minkowskiDistance(a.features.toBreeze, b.features.toBreeze, exponent)

  /**
    * @param name
    * @return Returns the DistanceFunction associated with specified name.
    */
  def from(name: String): (Any) => DistanceFunction = name match {
    case "L_infinity" => (_) => L_infinity
    case "cosine"     => (_) => cosine
    case "euclidean"  => (_) => euclidean
    case "manhattan"  => (_) => manhattan
    case "minkowski"  => (e: Any) => minkowski(e.asInstanceOf[Double])

    case _ => throw new IllegalArgumentException(s"unknown distance function $name")
   }

}
