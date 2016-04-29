package org.tmoerman.plongeur.tda

import java.lang.Math.sqrt

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Filters.reify
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.SparkContextSpec
import shapeless._

/**
  * @author Thomas Moerman
  */
class FiltersSpec extends FlatSpec with SparkContextSpec with Matchers {

  behavior of "materializing filter specs"

  it should "materialize a feature by index" in {
    val spec = "feature" :: 1 :: HNil

    val f: FilterFunction = reify(spec, null)

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

  it should "materialize L_inf centrality in function of default distance" in {
    val spec: HList = "centrality" :: "L_infinity" :: HNil

    val f = reify(spec, rdd)

    dataPoints.map(f).toSet shouldBe Set(sqrt(8))
  }

  it should "materialize L_inf centrality in function of specified no-args distance" in {
    val spec: HList = "centrality" :: "L_infinity" :: "euclidean" :: HNil

    val f = reify(spec, rdd)

    dataPoints.map(f).toSet shouldBe Set(sqrt(8))
  }

}
