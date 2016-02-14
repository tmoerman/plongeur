package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distance.euclidean
import org.tmoerman.plongeur.test.TestResources
import smile.clustering.HierarchicalClustering

import Clustering._

/**
  * @author Thomas Moerman
  */
class ClusteringSpec extends FlatSpec with TestResources with Matchers {

  behavior of "memoized cluster identifier"

  it should "return the same uuid for same input" in {

    val f = uuidClusterIdentifier

    f(0) shouldBe f(0)
    f(1) should not be f(2)

  }

  behavior of "clustering the data set with histogram partitioning heuristic"

  val data = heuristicLabeledPointsRDD.collect.toList

  it should "yield expected cluster labels" in {

    val distanceMatrix = distances(data, euclidean)

    val hierarchicalClustering = new HierarchicalClustering(linkage("single", distanceMatrix))

    val heights = hierarchicalClustering.getHeight

    val cutoff = histogramPartitionHeuristic()(heights)

    hierarchicalClustering.partition(cutoff) shouldBe Array(0, 0, 0, 0, 1, 1, 1, 1)

  }

  it should "yield correct clusters and members" in {

    println(cluster()(data).map(_.verbose).mkString("\n"))

  }

}
