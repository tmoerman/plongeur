package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.tda.cluster.Scale._

import TDAParams._

/**
  * @author Thomas Moerman
  */
class ModelSpec extends FlatSpec with Matchers {

  "initializing a DataPoint with meta data" should "allow Serializable values" in {
    DataPoint(
      index = 10,
      features = dense(1, 2, 3),
      meta = Some(Map("int" -> 1, "str" -> "a")))
  }

  "a FilterFunction" should "not be instantiable with an illegal overlap value" in {
    intercept[IllegalArgumentException] {
      Filter(Feature(0), 0, -100)
    }
  }

  behavior of "lenses"

  val base =
    TDAParams(
      lens = TDALens(
        Filter(Feature(0), 10, 0.6),
        Filter(Feature(1), 20, 0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  behavior of "lenses"

  it should "be able to update a filter" in {
    val updated = modFilter(0)(base).setTo(Filter(Feature(0), 30, 0.8))

    updated shouldBe TDAParams(
      lens = TDALens(
        Filter(Feature(0), 30, 0.8),
        Filter(Feature(1), 20, 0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))
  }

  it should "be able to update nrBins in a filter" in {
    setFilterNrBins(0, 100)(base) shouldBe TDAParams(
      lens = TDALens(
        Filter(Feature(0), 100, 0.6),
        Filter(Feature(1), 20,  0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))
  }

  it should "be able to update overlap in a filter" in {
    setFilterOverlap(1, 0.666)(base) shouldBe TDAParams(
      lens = TDALens(
        Filter(Feature(0), 10, 0.6),
        Filter(Feature(1), 20, 0.666)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))
  }

  it should "return the original when passed an invalid lens" in {
    val invalidIndex = 5

    setFilterOverlap(invalidIndex, 0.5)(base) shouldBe base
  }

  it should "be able to update subtypes of ScaleSelection" in {
    setScaleResolution(666)(base) shouldBe TDAParams(
      lens = TDALens(
        Filter(Feature(0), 10, 0.6),
        Filter(Feature(1), 20, 0.6)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(666))
  }

  "finding max of Vector" should "work" in {
    dense(1.0, 2.0, 3.0, 2.0).argmax shouldBe 2

    dense(1.0, 2.0, 3.0, 2.0).toSparse.argmax shouldBe 2

    val v = dense(1.0, 2.0, 3.0, 2.0)

    val max = v(v.argmax)
  }

}
