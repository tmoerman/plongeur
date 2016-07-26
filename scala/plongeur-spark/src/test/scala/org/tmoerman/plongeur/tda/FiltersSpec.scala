package org.tmoerman.plongeur.tda

import java.lang.Math.sqrt

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.SparkContextSpec
import shapeless._

/**
  * @author Thomas Moerman
  */
class FiltersSpec extends FlatSpec with SparkContextSpec with Matchers {

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

    val spec: HList = "eccentricity" :: 1 :: HNil

    val amended = toBroadcastAmendment(spec, ctx).map{ case (k, fn) => ctx.addBroadcast(k, fn) }.get

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set((2 + 2 + sqrt(8)) / 4)
  }

  it should "reify L_inf eccentricity in function of default distance" in {

    val ctx = TDAContext(sc, rdd)

    val spec: HList = "eccentricity" :: "infinity" :: HNil

    val amended = toBroadcastAmendment(spec, ctx).map{ case (k, fn) => ctx.addBroadcast(k, fn) }.get

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set(sqrt(8))
  }

  it should "reify L_inf eccentricity in function of specified no-args distance" in {

    val ctx = TDAContext(sc, rdd)

    val spec: HList = "eccentricity" :: "infinity" :: "euclidean" :: HNil

    val amended = toBroadcastAmendment(spec, ctx).map{ case (k, fn) => ctx.addBroadcast(k, fn) }.get

    val ff = toFilterFunction(spec, amended)

    dataPoints.map(ff).toSet shouldBe Set(sqrt(8))
  }

}
