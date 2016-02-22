package org.tmoerman.plongeur.tda

import breeze.linalg.DenseVector
import breeze.linalg.functions._

import org.apache.spark.mllib.linalg.VectorConversions._
import org.tmoerman.plongeur.tda.Model.{IndexedDataPoint, DataPoint}

/**
  * @author Thomas Moerman
  */
object Distance {

  type DistanceFunction = (DataPoint, DataPoint) => Double

  val chebyshev = (a: DataPoint, b: DataPoint) => chebyshevDistance(a.features.toBreeze, b.features.toBreeze)

  val cosine    = (a: DataPoint, b: DataPoint) => cosineDistance   (a.features.toBreeze, b.features.toBreeze)

  val euclidean = (a: DataPoint, b: DataPoint) => euclideanDistance(a.features.toBreeze, b.features.toBreeze)

  val manhattan = (a: DataPoint, b: DataPoint) => manhattanDistance(a.features.toBreeze, b.features.toBreeze)

  def minkowski(exponent: Double) = (a: DataPoint, b: DataPoint) => minkowskiDistance(a.features.toBreeze, b.features.toBreeze, exponent)

  def bla: Unit = {

    val a = DenseVector(1.0, 2.9)

    val r = minkowski(3)(IndexedDataPoint(1, a.toMLLib), IndexedDataPoint(2, a.toMLLib))

  }

}
