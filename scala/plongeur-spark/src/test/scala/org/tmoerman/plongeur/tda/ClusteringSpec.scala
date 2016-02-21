package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.regression.LabeledPoint
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.{FileResources, TestResources}

import Clustering._

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

  it should "yield expected cluster labels" in {

    clusterLabels(heuristicData) shouldBe Array(0, 0, 0, 0, 1, 1, 1, 1)

  }

  it should "yield correct clusters and members" in {

    println(cluster(heuristicData).map(_.verbose).mkString("\n"))

  }

  "clustering a singleton data set with histogram partitioning heuristic" should "return a single cluster" in {

    val singleton = List(LabeledPoint(66.6, dense(1.0, 2.0)))

    clusterLabels(singleton) shouldBe Array(0)

  }

  "clustering a pair of data with histogram partitioning heuristic" should "return two clusters" in {

    val pair = List(LabeledPoint(66.6, dense(1.0, 2.0)), LabeledPoint(77.7, dense(10.0, 20.0)))

    clusterLabels(pair) shouldBe Array(0, 1)
    
  }

}
