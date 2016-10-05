package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distance.EuclideanDistance
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch.{ApproximateMedian, ArithmeticMean, HashKey, RandomCandidate}
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class SketchSpec extends FlatSpec with SparkContextSpec with Matchers {

  val square = List(
    (0.0, 0.0), // lower row
    (1.0, 0.0),
    (2.0, 0.0),
    (3.0, 0.0),
    (4.0, 0.0),

    (0.0, 4.0), // upper row
    (1.0, 4.0),
    (2.0, 4.0),
    (3.0, 4.0),
    (4.0, 4.0),

    (0.0, 1.0), // left column
    (0.0, 2.0),
    (0.0, 3.0),

    (4.0, 1.0), // right column
    (4.0, 2.0),
    (4.0, 3.0))

  val center    = (2.0, 2.0)
  val offCenter = (1.9, 2.1)

  private def dps(coordinates: Seq[(Double, Double)]) =
    coordinates
      .zipWithIndex
      .map{ case ((x, y), idx) => dp(idx, dense(x, y)) }

  private def keyed(data: Seq[DataPoint]) =
    sc.parallelize(data)
      .keyBy(_ => "key".asInstanceOf[HashKey])

  "RandomCandidate" should "return a random data point" in {
    val data   = dps(square)
    val rdd    = keyed(data)
    val result = new RandomCandidate().apply(rdd).first

    result._1.toSet shouldBe (0 to 15).toSet

    data should contain (result._2)
  }

  "ArithmeticMean" should "return the center of gravity" in {
    val data   = dps(square)
    val rdd    = keyed(data)
    val result = ArithmeticMean.apply(rdd).first

    result._1.toSet shouldBe (0 to 15).toSet

    result._2.features shouldBe dense(2.0, 2.0)
  }

  behavior of "ApproximateMedian"

  val approximateMedian = new ApproximateMedian(4, EuclideanDistance)

  it should "correctly compute the exact winner" in {
    val data  = dps(offCenter :: square)
    val exact = approximateMedian.exactWinner(data)

    exact.features shouldBe dense(1.9, 2.1)
  }

  it should "return the one closest to the center of gravity" in {
    val data   = dps(center :: square)
    val rdd    = keyed(data)
    val result = approximateMedian.apply(rdd).first

    result._1.toSet shouldBe (0 to 16).toSet
  }

}