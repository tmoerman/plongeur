package org.tmoerman.plongeur.tda

import java.lang.Math.sqrt

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distance.EuclideanDistance
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch.{RandomCandidate, SketchParams}
import org.tmoerman.plongeur.test.SparkContextSpec
import shapeless._

/**
  * @author Thomas Moerman
  */
class FiltersSpec extends FlatSpec with SparkContextSpec with Matchers {

  implicit def toFilter(spec: HList) = Filter(spec)

  behavior of "toBroadcastKey"

  it should "return None when no broadcast is available for the filter" in {
    val filter = Filter("feature" :: 0 :: HNil)

    val broadcastKey = toBroadcastKey(filter)

    broadcastKey shouldBe None
  }

  it should "return filter key when no sketch is specified" in {
    val filter = Filter("eccentricity" :: 1 :: HNil)

    val broadcastKey = toBroadcastKey(filter)

    broadcastKey shouldBe Some("Eccentricity(n=1, distance=ManhattanDistance)")
  }

  it should "return filter key with sketch key if specified" in {
    val filter = Filter("eccentricity" :: 1 :: HNil, sketchParams = Some(SketchParams(10, 1.0, new RandomCandidate())))

    val broadcastKey = toBroadcastKey(filter)

    broadcastKey shouldBe Some("Eccentricity(n=1, distance=ManhattanDistance), Sketch(k=10, r=1.0, proto=RandomCandidate)")
  }

  behavior of "toSketchKey"

  it should "return the key for the sketch params in the filter" in {
    val filter = Filter("eccentricity" :: 1 :: HNil, sketchParams = Some(SketchParams(10, 1.0, new RandomCandidate())))

    val sketchKey = toSketchKey(filter)

    sketchKey shouldBe Some("Sketch(k=10, r=1.0, proto=RandomCandidate)")
  }

  behavior of "reifying filter specs"

  it should "reify a feature by index" in {
    val spec = "feature" :: 1 :: HNil

    val f: FilterFunction = toFilterFunction(spec, null)

    val dataPoint = (0, dense(1, 2, 3))

    f(dataPoint) shouldBe 2
  }

  val dataPoints =
    Seq(
      dp(0, dense(0, 0)),
      dp(1, dense(0, 2)),
      dp(2, dense(2, 0)),
      dp(3, dense(2, 2)))

  val rdd = sc.parallelize(dataPoints)

  it should "reify L_1 eccentricity" in {
    val ctx = TDAContext(sc, rdd)

    val spec: HList = "eccentricity" :: 1 :: "euclidean" :: HNil

    val amended = toContextAmendment(spec).apply(ctx)

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set((2 + 2 + sqrt(8)) / 4)
  }

  it should "reify L_inf eccentricity in function of default distance" in {
    val ctx = TDAContext(sc, rdd)

    val spec: HList = "eccentricity" :: "infinity" :: HNil

    val amended = toContextAmendment(spec).apply(ctx)

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set(4.0)
  }

  it should "reify L_inf eccentricity in function of specified no-args distance" in {
    val ctx = TDAContext(sc, rdd)

    val spec: HList = "eccentricity" :: "infinity" :: "euclidean" :: HNil

    val amended = toContextAmendment(spec).apply(ctx)

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set(sqrt(8))
  }

  it should "reify L_inf different filter functions for different specs" in {
    val ctx = TDAContext(sc, rdd)

    val spec1: HList = "eccentricity" :: "infinity" :: "euclidean" :: HNil
    val spec2: HList = "eccentricity" :: 1 :: HNil

    val amended = (toContextAmendment(spec1) andThen toContextAmendment(spec2)).apply(ctx)

    val ff1 = toFilterFunction(spec1, amended)
    val ff2 = toFilterFunction(spec2, amended)

    dataPoints.map(ff1).toSet shouldBe Set(sqrt(8))
    dataPoints.map(ff2).toSet shouldBe Set(2.0)
  }

  behavior of "Maps vs. SparseVectors"

  it should "yield equal results" in {
    val ctx = TDAContext(sc, rdd)

    val ns = "infinity" :: 1 :: 2 :: 3 :: HNil

    ns
      .toList
      .foreach(n => {
        val map = Filters.eccentricityMap(n, ctx, EuclideanDistance)
        val vec = Filters.eccentricityVec(n, ctx, EuclideanDistance)

        map.foreach{ case (i, v) => vec.apply(i) shouldBe v }
      })

  }

}
