package org.tmoerman.plongeur.tda

import breeze.linalg.DenseVector
import breeze.linalg.functions._
import org.apache.spark.mllib.regression.LabeledPoint

import org.apache.spark.mllib.linalg.VectorConversions._

/**
  * @author Thomas Moerman
  */
object Distance {

  type DistanceFunction = (LabeledPoint, LabeledPoint) => Double

  val chebyshev = (a: LabeledPoint, b: LabeledPoint) => chebyshevDistance(a.features.toBreeze, b.features.toBreeze)

  val cosine    = (a: LabeledPoint, b: LabeledPoint) => cosineDistance   (a.features.toBreeze, b.features.toBreeze)

  val euclidean = (a: LabeledPoint, b: LabeledPoint) => euclideanDistance(a.features.toBreeze, b.features.toBreeze)

  val manhattan = (a: LabeledPoint, b: LabeledPoint) => manhattanDistance(a.features.toBreeze, b.features.toBreeze)

  def minkowski(exponent: Double) = (a: LabeledPoint, b: LabeledPoint) => minkowskiDistance(a.features.toBreeze, b.features.toBreeze, exponent)

  def bla: Unit = {

    val a = DenseVector(1.0, 2.9)

    val r = minkowski(3)(LabeledPoint(1.0, a.toMLLib), LabeledPoint(2.0, a.toMLLib))

  }

}
