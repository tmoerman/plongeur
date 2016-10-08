package org.tmoerman.lab

import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * @author Thomas Moerman
  */
class MLLab extends FlatSpec with Matchers with SparkContextSpec with TestResources {

  behavior of "columnSimilarities"

  it should "compute similarities between the dimensions" in {
    val sims = new RowMatrix(circle1kRDD.map(_.features)).columnSimilarities()
  }

  behavior of "distance matrix"

  it should "be computable" in {
    val in = circle1kRDD.distinctComboSets

    println(in.take(3).mkString("\n"))
  }

}