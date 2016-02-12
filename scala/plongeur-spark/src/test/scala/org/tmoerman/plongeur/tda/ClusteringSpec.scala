package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.TestResources
import smile.clustering.HierarchicalClustering

import Clustering._
import smile.math.distance.EuclideanDistance

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

  behavior of "clustering"

  it should "bla" in {

//    val points =
//      pointsRDD
//        ._2
//        .collect
//        .map{ case (x, y, _) => Array(x, y) }.zipWithIndex
//
//    val dist = distanceMatrix(points.toList, new EuclideanDistance)
//
//    val clustering: HierarchicalClustering = cluster(points, dist, uuidClusterIdentifier)
//
//    val labels = clustering.partition(9)
//
//    print(labels.zipWithIndex.map{ case (label, idx) => s"$idx,$label" }.mkString("\n"))

  }

}
