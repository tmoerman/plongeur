package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.tda.cluster.Scale._
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class ModelSpec extends FlatSpec with Matchers {

  "initializing a DataPoint with meta data" should "allow Serializable values" in {
    IndexedDataPoint(
      index = 10,
      features = dense(1, 2, 3),
      meta = Some(Map("int" -> 1,
                      "str" -> "a")))
  }

  "a FilterFunction" should "not be instantiable with an illegal overlap value" in {
    intercept[IllegalArgumentException] {
      Filter(HNil, 0, -100)
    }
  }

  behavior of "lenses"

  val base =
    TDAParams(
      lens = TDALens(
        Filter("feature" :: 0 :: HNil, 10, 0.6),
        Filter("feature" :: 1 :: HNil, 20, 0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  behavior of "lenses"

  import L._

  it should "be able to update a filter" in {
    val updated = filter(0).set(base, Filter("feature" :: 0 :: HNil, 30, 0.8)).get

    updated shouldBe TDAParams(
      lens = TDALens(
        Filter("feature" :: 0 :: HNil, 30, 0.8),
        Filter("feature" :: 1 :: HNil, 20, 0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))
  }

  it should "be able to update nrBins in a filter" in {
    val updated = setNrBins(0, 100)(base)

    updated shouldBe TDAParams(
      lens = TDALens(
        Filter("feature" :: 0 :: HNil, 100, 0.6),
        Filter("feature" :: 1 :: HNil, 20,  0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))
  }

}
