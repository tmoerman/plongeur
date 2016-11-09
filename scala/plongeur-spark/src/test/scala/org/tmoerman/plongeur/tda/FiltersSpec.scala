package org.tmoerman.plongeur.tda

import java.lang.Math.sqrt

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch.{RandomCandidate, SketchParams}
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class FiltersSpec extends FlatSpec with SparkContextSpec with Matchers {

  behavior of "toFilterSpecKey"

  implicit val seed: Long = 666

  it should "yield a Some(_) in cases" in {
    val pc0 = PrincipalComponent(0)
    toFilterSpecKey(pc0) shouldBe Some(pc0)

    val ecc = Eccentricity(Left(1))
    toFilterSpecKey(ecc) shouldBe Some(ecc)

    val den = Density(1.0)
    toFilterSpecKey(den) shouldBe Some(den)
  }

  it should "yield 0 in cases" in {
    val feat0 = Feature(0)
    toFilterSpecKey(feat0) shouldBe None
  }

  behavior of "toBroadcastKey"

  it should "return None when no broadcast is available for the filter" in {
    val filter = Filter(Feature(0))

    val broadcastKey = toBroadcastKey(filter)

    broadcastKey shouldBe None
  }

  it should "return filter key when no sketch is specified" in {
    val filter = Filter(Eccentricity(Left(1)))

    val broadcastKey = toBroadcastKey(filter)

    broadcastKey shouldBe Some(filter.spec)
  }

  it should "return filter key with sketch key if specified" in {
    val filter = Filter(Eccentricity(Left(1)), sketch = Some(SketchParams(LSHParams(10, Some(1.0)), new RandomCandidate())))

    val broadcastKey = toBroadcastKey(filter)

    broadcastKey shouldBe Some((filter.spec, filter.sketch.get))
  }

  behavior of "toSketchKey"

  it should "return the key for the sketch params in the filter" in {
    val filter = Filter(Eccentricity(Left(1)), sketch = Some(SketchParams(LSHParams(10, Some(1.0)), new RandomCandidate())))

    val sketchKey = toSketchKey(filter)

    sketchKey shouldBe filter.sketch
  }

  behavior of "reifying filter specs"

  val dataPoints =
    Seq(
      dp(0, dense(0, 0)),
      dp(1, dense(0, 2)),
      dp(2, dense(2, 0)),
      dp(3, dense(2, 2)))

  val rdd = sc.parallelize(dataPoints)

  it should "reify a feature by index" in {
    val f = toFilterFunction(Feature(1), ctx)

    val dataPoint = (0, dense(1, 2, 3))

    f(dataPoint) shouldBe 2
  }

  val ctx = TDAContext(sc, rdd)

  it should "reify L_1 eccentricity" in {
    val spec = Eccentricity(Left(1), distance = EuclideanDistance) // "eccentricity" :: 1 :: "euclidean" :: HNil

    val amended = toContextAmendment(spec).apply(ctx)

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set((2 + 2 + sqrt(8)) / 4)
  }

  it should "reify L_inf eccentricity in function of default distance" in {
    val spec = Eccentricity(Right(INFINITY))

    val amended = toContextAmendment(spec).apply(ctx)

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set(4.0)
  }

  it should "reify L_inf eccentricity in function of specified no-args distance" in {
    val spec = Eccentricity(Right(INFINITY), distance = EuclideanDistance)

    val amended = toContextAmendment(spec).apply(ctx)

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set(sqrt(8))
  }

  it should "reify L_inf different filter functions for different specs" in {
    val spec1 = Eccentricity(Right(INFINITY), distance = EuclideanDistance)
    val spec2 = Eccentricity(Left(1))

    val amended = (toContextAmendment(spec1) andThen toContextAmendment(spec2)).apply(ctx)

    val ff1 = toFilterFunction(spec1, amended)
    val ff2 = toFilterFunction(spec2, amended)

    dataPoints.map(ff1).toSet shouldBe Set(sqrt(8))
    dataPoints.map(ff2).toSet shouldBe Set(2.0)
  }

  behavior of "Maps vs. SparseVectors"

  it should "yield equal results" in {
    val ps = Right(INFINITY) :: Left(1) :: Left(2) :: Left(3) :: Nil

    ps
      .toList
      .foreach(p => {
        val map = Filters.eccentricityMap(p, ctx, EuclideanDistance)
        val vec = Filters.eccentricityVec(p, ctx, EuclideanDistance)

        map.foreach{ case (i, v) => vec.apply(i) shouldBe v }
      })
  }

}
