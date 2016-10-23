package org.tmoerman.lab

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.test.FileResources
import smile.clustering.HierarchicalClustering
import smile.clustering.linkage.SingleLinkage

/**
  * @author Thomas Moerman
  */
class SmileLab extends FlatSpec with FileResources with Matchers {

  behavior of "partition"

  it should "yay" in {

    val distances = distanceMatrix(heuristicData, EuclideanDistance)

    val linkage = new SingleLinkage(distances)

    val clustering = new HierarchicalClustering(linkage)

    val membership = clustering.partition(4)



  }

}