package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.{ManhattanDistance, EuclideanDistance}
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch._
import org.tmoerman.plongeur.test.{TestResources, SparkContextSpec}

/**
  * @author Thomas Moerman
  */
class SketchSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

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
    sc.parallelize(data).keyBy(_ => 666)

  behavior of "RandomCandidate"

  it should "return a random data point from a non-trivial data set" in {
    val data   = dps(square)
    val rdd    = keyed(data)
    val result = new RandomCandidate().apply(rdd).first

    result._1.toSet shouldBe (0 to 15).toSet

    data should contain (result._2)
  }

  behavior of "ArithmeticMean"

  it should "return the center of gravity" in {
    val data   = dps(square)
    val rdd    = keyed(data)
    val result = ArithmeticMean.apply(rdd).first

    result._1.toSet shouldBe (0 to 15).toSet

    result._2.features shouldBe dense(2.0, 2.0)
  }

  behavior of "ApproximateMedian"

  val approximateMedian = new ApproximateMedian(4)

  it should "correctly compute the exact winner from a singleton" in {
    val data  = dps(center :: Nil)
    val exact = approximateMedian.exactWinner(EuclideanDistance)(data)

    exact.features shouldBe dense(2.0, 2.0)
  }

  it should "correctly compute the exact winner" in {
    val data  = dps(center :: square)
    val exact = approximateMedian.exactWinner(EuclideanDistance)(data)

    exact.features shouldBe dense(2.0, 2.0)
  }

  it should "return a candidate with the correct indices" in {
    val data   = dps(center :: square)
    val rdd    = keyed(data)
    val result = approximateMedian.apply(rdd).first

    result._1.toSet shouldBe (0 to 16).toSet
  }

  it should "return a candidate from a singleton" in {
    val data   = dps(center :: Nil)
    val rdd    = keyed(data)
    val result = approximateMedian.apply(rdd).first

    result._1.toSet    shouldBe Set(0)
    result._2.features shouldBe dense(2.0, 2.0)
  }

  behavior of "Sketch"

  it should "pass the smoke test for Iris dataset" in {
    val k = 2
    val r = 1.0

    val ctx = TDAContext(sc, irisDataPointsRDD)

    val lshParams    = LSHParams(k, Some(r))
    val sketchParams = SketchParams(lshParams, new ApproximateMedian(5))

    val sketch = Sketch(ctx, sketchParams)

    sketch.N should be < ctx.N

    // sketch.originLookup.size shouldBe sketch.N
  }

}