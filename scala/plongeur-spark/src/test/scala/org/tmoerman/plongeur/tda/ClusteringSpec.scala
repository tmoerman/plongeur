package org.tmoerman.plongeur.tda

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

  behavior of "clustering the heuristic data set with histogram partitioning heuristic"

  val scaleSelection = histogram(10)

//  it should "specify heights" in {
//
//    val distances = distanceMatrix(heuristicData, euclidean)
//
//    val linkage = createLinkage(SINGLE, distances)
//
//    val hierarchicalClustering = new HierarchicalClustering(linkage)
//
//    val cutoffHeight = partitionHeuristic(hierarchicalClustering.getHeight)
//
//    val heights = hierarchicalClustering.getHeight
//
//    val tree = hierarchicalClustering.getTree
//
//    println(heights.mkString("\n"))
//
//  }

  it should "yield expected cluster labels" in {

    SmileClusteringProvider.apply(heuristicData).labels(scaleSelection) shouldBe Array(0, 0, 0, 0, 1, 1, 1, 1)

  }

//  it should "yield correct clusters and members" in {
//
//    println(doClustering(dataPoints = heuristicData).map(_.verbose).mkString("\n"))
//
//  }
//
//  "clustering a singleton data set with histogram partitioning heuristic" should "return a single cluster" in {
//
//    val singleton = List(IndexedDataPoint(66, dense(1.0, 2.0)))
//
//    clusterLabels(singleton) shouldBe Array(0)
//
//  }
//
//  "clustering a pair of data with histogram partitioning heuristic" should "return two clusters" in {
//
//    val pair =
//      List(
//        IndexedDataPoint(66, dense(1.0, 2.0)),
//        IndexedDataPoint(77, dense(10.0, 20.0)))
//
//    clusterLabels(pair) shouldBe Array(0, 1)
//
//  }

}
