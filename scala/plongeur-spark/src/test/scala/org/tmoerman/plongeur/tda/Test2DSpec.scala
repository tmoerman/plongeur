package org.tmoerman.plongeur.tda

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.test.TestResources

import Covering._
import Model._
import TDA._
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class Test2DSpec extends FlatSpec with TestResources with Matchers {

  behavior of "Covering the points"

  it should "associate points with the correct HyperCubeCoordinateVectors" in {
    val lens =
      TDALens(
        Filter("feature" :: 0 :: HNil, 1, 0.5),
        Filter("feature" :: 1 :: HNil, 1, 0.5))

    val size = 12.0

    val boundaries = Array((0.0, size), (0.0, size))

    val filterFunctions = lens.filters.map(f => Filters.toFilterFunction(f.spec, test2DLabeledPointsRDD))

    val covering = levelSetsInverseFunction(boundaries, lens, filterFunctions)

    val result = test2DLabeledPointsRDD.flatMap(p => covering(p).map(k => (k, p))).collect

    result
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
