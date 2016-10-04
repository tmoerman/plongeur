package org.tmoerman.plongeur.tda.cluster

import java.lang.Math.sqrt

import org.apache.spark.mllib.linalg.Vectors.dense
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.test.FileResources

/**
  * @author Thomas Moerman
  */
class ClusteringSpec extends FlatSpec with FileResources with Matchers {

  behavior of "clustering with histogram(10) scale selection"

  it should "yield 1 cluster for homogeneous data points" in {
    val homogeneous = heuristicData.take(4)

    val clustering = SimpleSmileClusteringProvider.apply(homogeneous)

    clustering.heights() shouldBe Seq(1.0, 1.0, 1.0, sqrt(2))

    clustering.labels(histogram(10)) shouldBe Seq(0, 0, 0, 0)
  }

  it should "yield 2 cluster for bipartite data points" in {
    val clustering = SimpleSmileClusteringProvider.apply(heuristicData)

    println(clustering.debug)

    clustering.heights(true) shouldBe Seq(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, sqrt(8), 5.0)
    clustering.heights(false) shouldBe Seq(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, sqrt(8))

    clustering.labels(histogram(10)) shouldBe Seq(0, 0, 0, 0, 1, 1, 1, 1)
    clustering.labels(firstGap(30)) shouldBe Seq(0, 0, 0, 0, 1, 1, 1, 1)
  }

  it should "yield 1 cluster for a singleton" in {
    val singleton = heuristicData.take(1)

    val clustering = SimpleSmileClusteringProvider.apply(singleton)

    clustering.labels(histogram(10)) shouldBe Seq(0)
  }

  it should "yield 2 clusters for a pair" in {
    val pair = heuristicData.take(2)

    val clustering = SimpleSmileClusteringProvider.apply(pair)

    clustering.labels(histogram(10)) shouldBe Seq(0, 1)
  }

  it should "yield an acceptable nr of clusters for test.2d.csv data" in {
    val clustering = SimpleSmileClusteringProvider.apply(test2dData)

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

  behavior of "toLocalCluster"

  it should "map cluster labels to cluster identifiers correctly" in {

    val dataPoints: Seq[DataPoint] = (1 to 5).map(i => dp(i, dense(i)))
    val labels = Seq(0, 1, 2, 1, 0)

    val local = Clustering.localClusters(null, dataPoints, labels)

    local.map(_.dataPoints.map(_.index).toSet).toSet shouldBe Set(Set(1, 5), Set(2, 4), Set(3))

  }

}
