package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Filters.materialize
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.SparkContextSpec
import shapeless._

/**
  * @author Thomas Moerman
  */
class FiltersSpec extends FlatSpec with SparkContextSpec with Matchers {

  behavior of "materializing filter specs"

  it should "correctly materialize a feature by index" in {

    val spec = "feature" :: 1 :: HNil

    val f: FilterFunction = materialize(spec, null)

    val dataPoint = (0, dense(1, 2, 3))

    f(dataPoint) shouldBe 2
  }

//  it should "correctly materialize a centrality distance" in {
//
//    val spec: HList = "centrality" :: "L_infinity" :: HNil
//
//    val dataPoints =
//      Seq(
//        dp(0, dense(0, 0)),
//        dp(1, dense(0, 2)),
//        dp(2, dense(2, 0)),
//        dp(3, dense(2, 2)))
//
//    val tdaContext = TDAContext(sc.parallelize(dataPoints))
//
//    val f: FilterFunction = materialize(spec, tdaContext)
//
//    dataPoints.map(f).toSet shouldBe Set(2.5)
//  }

}
