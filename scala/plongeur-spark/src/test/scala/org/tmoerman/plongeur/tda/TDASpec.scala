package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Brewer.palettes
import org.tmoerman.plongeur.tda.Colour._
import org.tmoerman.plongeur.tda.Inspections._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}

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

  it should "work in a smoke test" in {

    val tdaParams =
      TDAParams(
        lens = TDALens(
          Filter(Feature(0), 1, 0.5),
          Filter(Feature(1), 1, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, test2DLabeledPointsRDD))

    val all = test2DLabeledPointsRDD.distinct.collect.toSet

    result.clustersRDD.flatMap(_.dataPoints).distinct.collect.toSet shouldBe all

    printInspections(result, "test2D")
  }

  it should "recover the 100 entries circle topology" in {
    val tdaParams =
      TDAParams(
        lens = TDALens(Filter(Feature(0), 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, circle250RDD))

    printInspections(result, "circle250")

    // TODO verify stuff
  }

  it should "pass smoke test with eccentricity filter" in {
    val tdaParams =
      TDAParams(
        lens = TDALens(Filter(Eccentricity(Left(1)), 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, circle250RDD))
  }

  it should "pass smoke test with pca filter" in {
    val tdaParams =
      TDAParams(
        lens = TDALens(Filter(PrincipalComponent(0), 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val result = TDAProcedure.apply(tdaParams, TDAContext(sc, circle250RDD))
  }

  it should "pass smoke test with colouring" in {
    val ctx = TDAContext(sc, irisDataPointsRDD)

    val setosa = (d: DataPoint) => d.meta.get("cat") == "setosa"

    val tdaParams =
      TDAParams(
        lens = TDALens(Filter(PrincipalComponent(0), 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10),
        colouring = ClusterPercentage(palettes("Blues")(9), setosa))

    val result = TDAProcedure.apply(tdaParams, ctx)
  }

}
