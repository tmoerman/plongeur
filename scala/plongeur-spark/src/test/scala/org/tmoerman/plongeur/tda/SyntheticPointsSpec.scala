package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.{DenseVector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.TestResources

import Skeleton._

/**
  * @author Thomas Moerman
  */
class SyntheticPointsSpec extends FlatSpec with TestResources with Matchers {

  behavior of "Covering the points"

  it should "associate points with the correct HyperCubeCoordinateVectors" in {

    val labeledPoints =
      pointsRDD
        ._2
        .map{ case (x, y, cat) => LabeledPoint(cat, Vectors.dense(x, y)) }

    val lens = Lens(Filter(feature(0), 1, 0.5),
                    Filter(feature(1), 1, 0.5)
    )

    val size = 12.0

    val boundaries = Array((0.0, size), (0.0, size))

    val covering = coveringFunction(lens, boundaries)

    val result = labeledPoints.flatMap(p => covering(p).map(k => (k, p))).collect

    result
      .foreach{ case (hyperCubeCoordinateVector: Vector[Any], LabeledPoint(cluster, features: DenseVector)) =>

        def testCoveringContainsPoint(i: Int): Unit = {
          val coordinate = hyperCubeCoordinateVector(i).asInstanceOf[BigDecimal].toDouble

          coordinate        should be <= features(i)
          coordinate + size should be >  features(i)
        }

        testCoveringContainsPoint(0)
        testCoveringContainsPoint(1)

      }

  }

}
