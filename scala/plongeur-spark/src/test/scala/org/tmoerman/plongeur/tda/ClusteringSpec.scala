package org.tmoerman.plongeur.tda

import java.lang.Math.sqrt

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.tda.cluster._
import org.tmoerman.plongeur.test.FileResources

/**
  * @author Thomas Moerman
  */
class ClusteringSpec extends FlatSpec with FileResources with Matchers {

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

  it should "yield an acceptable nr of clusters for test.2d.csv data" in {
    val clustering = SmileClusteringProvider.apply(test2dData)

    println(clustering.heights(true))

    val resolutions =
      Stream
        .from(1)
        .map(i => (i, clustering.labels(histogram(i)).distinct.size))
        .takeWhile(_._1 <= 100)
        .distinct.sorted
        .mkString("\n")

    println(resolutions)

    clustering.labels(histogram(100)).distinct.size shouldBe 11
  }

}
