package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch.{HashKey, RandomCandidate}
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class SketchSpec extends FlatSpec with SparkContextSpec with Matchers {

  val coords = Seq(
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
    (4.0, 3.0),

    (1.5, 2.5)  // quasi center
  )

  val data = coords.zipWithIndex.map{ case ((x, y), idx) => dp(idx, dense(x, y)) }

  val rdd = sc.parallelize(data).cache

  "RandomCandidate" should "return a random data point" in {
    val keyed = rdd.keyBy(_ => "key".asInstanceOf[HashKey]) // all same key

    val result = new RandomCandidate().apply(keyed).first

    result._1.toSet shouldBe (0 to 16).toSet
    data should contain (result._2)
  }

  



}