package org.tmoerman.plongeur.tda

import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Covering._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.TestResources

/**
  * @author Thomas Moerman
  */
class Test2DSpec extends FlatSpec with SharedSparkContext with TestResources with Matchers {

  behavior of "Covering the points"

  it should "associate points with the correct HyperCubeCoordinateVectors" ignore {
    val lens =
      TDALens(
        Filter(Feature(0), 1, 0.5),
        Filter(Feature(1), 1, 0.5))

    val size = 12

    val ctx = TDAContext(sc, test2DLabeledPointsRDD)

    val result = levelSetInverseRDD(ctx, lens)

    result
      .collect
      .foreach{ case (hyperCubeCoordinateVector: Vector[BigDecimal], p: DataPoint) =>

        def testCoveringContainsPoint(i: Int): Unit = {
          val coordinate = hyperCubeCoordinateVector(i).toDouble

          coordinate        should be <= p.features(i)
          coordinate + size should be >  p.features(i)
        }

        testCoveringContainsPoint(0)
        testCoveringContainsPoint(1)
      }
  }

}