package org.tmoerman.plongeur.tda

import java.lang.Math.sqrt

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.tda.cluster._
import org.tmoerman.plongeur.test.FileResources

/**
  * @author Thomas Moerman
  */
class ClusteringSpec extends FlatSpec with FileResources with Matchers {

  behavior of "memoized cluster identifier"

  it should "return the same uuid for same input" in {

    val f = uuidClusterIdentifier

    f(0) shouldBe f(0)
    f(1) should not be f(2)

  }

  behavior of "clustering with histogram(10) scale selection"

  it should "yield 1 cluster for homogeneous data points" in {
    val homogeneous = heuristicData.take(4)

    val clustering = SmileClusteringProvider.apply(homogeneous)

    clustering.heights() shouldBe Seq(1.0, 1.0, 1.0, sqrt(2))

    clustering.labels(histogram(10)) shouldBe Seq(0, 0, 0, 0)
  }

  it should "yield 2 cluster for bipartite data points" in {
    val clustering = SmileClusteringProvider.apply(heuristicData)

    clustering.heights() shouldBe Seq(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, sqrt(8), 5.0)

    clustering.labels(histogram(10)) shouldBe Seq(0, 0, 0, 0, 1, 1, 1, 1)
  }

  it should "yield 1 cluster for a singleton" in {
    val singleton = heuristicData.take(1)

    val clustering = SmileClusteringProvider.apply(singleton)

    clustering.labels(histogram(10)) shouldBe Seq(0)
  }

  it should "yield 2 clusters for a pair" in {
    val pair = heuristicData.take(2)

    val clustering = SmileClusteringProvider.apply(pair)

    clustering.labels(histogram(10)) shouldBe Seq(0, 1)
  }

}
