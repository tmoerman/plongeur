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

  behavior of "toFilterKey"

  implicit val seed: Long = 666

  it should "yield the correct key" in {
    val feat = Feature(2)
    feat.key shouldBe feat

    val pc0 = PrincipalComponent(0)
    pc0.key shouldBe pc0.copy(n = -1)

    val lap = LaplacianEigenVector(0)
    lap.key shouldBe lap.copy(n = -1)

    val ecc = Eccentricity(Left(1))
    ecc.key shouldBe ecc

    val den = Density(1.0)
    den.key shouldBe den
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

  val ctx = TDAContext(sc, rdd)

  it should "make a filterRDD factory for the Feature(n) lens" in {
    val spec = Feature(0)

    toFilterRDDFactory(spec, ctx)
      .apply(spec)
      .collect
      .toMap shouldBe Map(0 -> 0d, 1 -> 0d, 2 -> 2d, 3 -> 2d)
  }

  it should "reify L_1 eccentricity" in {
    val spec = Eccentricity(Left(1), distance = EuclideanDistance) // "eccentricity" :: 1 :: "euclidean" :: HNil

    val amended = toContextAmendment(spec).apply(ctx)

    toFilterRDDFactory(spec, ctx)
      .apply(spec)
      .values
      .collect
      .toSet shouldBe Set((2 + 2 + sqrt(8)) / 4)
  }

  it should "reify L_inf eccentricity in function of default distance" in {
    val spec = Eccentricity(Right(INFINITY))

    val amendedCtx = toContextAmendment(spec).apply(ctx)

    toFilterRDDFactory(spec, amendedCtx).apply(spec).values.collect.toSet shouldBe Set(4.0)
  }

  it should "reify L_inf eccentricity in function of specified no-args distance" in {
    val spec = Eccentricity(Right(INFINITY), distance = EuclideanDistance)

    val amendedCtx = toContextAmendment(spec).apply(ctx)

    toFilterRDDFactory(spec, amendedCtx).apply(spec).values.collect.toSet shouldBe Set(sqrt(8))
  }

  it should "reify L_inf different filter functions for different specs" in {
    val spec1 = Eccentricity(Right(INFINITY), distance = EuclideanDistance)
    val spec2 = Eccentricity(Left(1))

    val amendedCtx = (toContextAmendment(spec1) andThen toContextAmendment(spec2)).apply(ctx)

    toFilterRDDFactory(spec1, amendedCtx).apply(spec1).values.collect.toSet shouldBe Set(sqrt(8))
    toFilterRDDFactory(spec2, amendedCtx).apply(spec1).values.collect.toSet shouldBe Set(2.0)
  }

}
