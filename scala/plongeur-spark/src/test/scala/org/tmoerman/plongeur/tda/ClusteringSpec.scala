package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distance.euclidean
import org.tmoerman.plongeur.test.TestResources
import smile.clustering.HierarchicalClustering

import Clustering._

import scala.math.sqrt

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

  behavior of "clustering the data set for the partitioning heuristic"

  it should "yield 2 clusters" in {

    val data = heuristicLabeledPointsRDD.collect.toList

    val distanceMatrix = distances(data, euclidean)

    val hierarchicalClustering = new HierarchicalClustering(linkage("single", distanceMatrix))

    val heights = hierarchicalClustering.getHeight

    val bla = (1 to 10).map(_ * 5).map(k => histogramPartitionHeuristic(k)(heights))

    println(hierarchicalClustering.partition(bla(0)).mkString("\n"))

    //print(labels.zipWithIndex.map{ case (label, idx) => s"$idx,$label" }.mkString("\n"))

  }

  behavior of "cluster partition heuristic"

  it should "boo" in {

    val heights = Array[Double](1, 1, 1, 1, 1, 1, sqrt(8.0))

    println(histogramPartitionHeuristic(10)(heights))

  }

}
