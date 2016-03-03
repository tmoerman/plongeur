package org.tmoerman.plongeur.tda

import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Inspections._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.TDA.TDAResult
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}

/**
  * @author Thomas Moerman
  */
class TDASpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  behavior of "TDA"

  implicit val counter = mapToInt

  def printInspections(result: TDAResult[UUID], name: String): Unit = {
    println(
      Seq(
        result.levelSetsToClusters.mkString("\n"),
        result.pointsToClusters.mkString("\n"),
        result.dotGraph(name)
      ).mkString("\n"))
  }

  it should "work with specified boundaries" in {

    val boundaries = Array((0.0, 12.0), (0.0, 12.0))

    val lens = Lens(Filter((p: DataPoint) => p.features(0), 1.0, 0.5),
                    Filter((p: DataPoint) => p.features(1), 1.0, 0.5))

    val result =
      TDA.execute(
        lens = lens,
        dataRDD = test2DLabeledPointsRDD,
        scaleSelection = histogram(100),
        clusterIdentifier = uuidClusterIDGenerator,
        coveringBoundaries = Some(boundaries))

    printInspections(result, "test2D")
  }

//  it should "work with calculated boundaries" in {
//
//    val lens = Lens(Filter((p: DataPoint) => p.features(0), 1.0, 0.5),
//                    Filter((p: DataPoint) => p.features(1), 1.0, 0.5))
//
//    val result =
//      Skeleton.execute(
//        lens = lens,
//        data = test2DLabeledPointsRDD)
//
//    val intro = result.clusterPoints
//
//    println(result.dotGraph("test2Dc"))
//  }

  it should "recover the 100 entries circle topology" in {

    val lens = Lens(Filter((p: DataPoint) => p.features(0), 0.10, 0.5))

    val result =
      TDA.execute(
        lens = lens,
        dataRDD = circle250RDD,
        scaleSelection = histogram(10),
        clusterIdentifier = uuidClusterIDGenerator)

    printInspections(result, "circle250")
  }


}
