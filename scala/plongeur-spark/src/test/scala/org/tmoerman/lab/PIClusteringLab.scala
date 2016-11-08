package org.tmoerman.lab

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class PIClusteringLab extends FlatSpec with SparkContextSpec with Matchers {



}

object PIClusteringLab {

  def apply(nrCircles: Int,
            nrPoints: Int,
            maxIterations: Int) = {



    ???
  }

  def generateCircle(radius: Double, n: Int): Seq[(Double, Double)] = {
    Seq.tabulate(n) { i =>
      val theta = 2.0 * math.Pi * i / n
      (radius * math.cos(theta), radius * math.sin(theta))
    }
  }

  def generateCirclesRdd(sc: SparkContext,
                         nCircles: Int,
                         nPoints: Int): RDD[(Long, Long, Double)] = {

    val points = (1 to nCircles)
      .flatMap(i => generateCircle(i, i * nPoints))
      .zipWithIndex

    val rdd = sc.parallelize(points)

    rdd.cartesian(rdd).flatMap { case (((x0, y0), i0), ((x1, y1), i1)) =>
      if (i0 < i1) {
        Some((i0.toLong, i1.toLong, gaussianSimilarity((x0, y0), (x1, y1))))
      } else {
        None
      }
    }
  }

  /**
    * Gaussian Similarity:  http://en.wikipedia.org/wiki/Radial_basis_function_kernel
    */
  def gaussianSimilarity(p1: (Double, Double), p2: (Double, Double)): Double = {
    val ssquares = (p1._1 - p2._1) * (p1._1 - p2._1) + (p1._2 - p2._2) * (p1._2 - p2._2)
    math.exp(-ssquares / 2.0)
  }

}