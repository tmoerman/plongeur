package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Inspections._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class TDASpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  behavior of "TDA algorithm"

  implicit val counter = mapToInt

  def printInspections(result: TDAResult, name: String) = {
    println(
      Seq(result.levelSetsToClusters.mkString("\n"),
          result.pointsToClusters.mkString("\n"),
          result.dotGraph(name))
        .mkString("\n"))
  }

  it should "work with specified boundaries" in {

    val tdaParams =
      TDAParams(
        lens = TDALens(
          Filter("feature" :: 0 :: HNil, 1, 0.5),
          Filter("feature" :: 1 :: HNil, 1, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10),
        coveringBoundaries = Some(Array((0.0, 12.0), (0.0, 12.0))))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, test2DLabeledPointsRDD))

    val all = test2DLabeledPointsRDD.distinct.collect.toSet

    result.clustersRDD.flatMap(_.dataPoints).distinct.collect.toSet shouldBe all

    printInspections(result, "test2D")
  }

  it should "recover the 100 entries circle topology" in {
    val tdaParams =
      TDAParams(
        lens = TDALens(Filter("feature" :: 0 :: HNil, 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, circle250RDD))

    printInspections(result, "circle250")

    // TODO verify stuff
  }

  it should "pass smoke test with eccentricity filter" in {
    val tdaParams =
      TDAParams(
        lens = TDALens(Filter("eccentricity" :: 1 :: HNil, 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, circle250RDD))
  }

  it should "pass smoke test with pca filter" in {
    val tdaParams =
      TDAParams(
        lens = TDALens(Filter("PCA" :: 0 :: HNil, 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, circle250RDD))
  }

}
