package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Inspections._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}
import rx.lang.scala.Observable
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class TDAMachineSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  behavior of "TDA Machine"

  implicit val counter = mapToInt

  def printInspections(result: TDAResult, name: String) = {
    println(
      Seq(result.levelSetsToClusters.mkString("\n"),
          result.pointsToClusters.mkString("\n"),
          result.dotGraph(name))
        .mkString("\n"))
  }

  it should "work with one input" in {
    val inParams =
      TDAParams(
        lens = TDALens(Filter("feature" :: 0 :: HNil, 0.10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val (outParams, result) =
      TDAMachine.run(TDAContext(sc, circle250RDD), Observable.just(inParams))
        .toBlocking
        .single

    inParams shouldBe outParams

    printInspections(result, "test TDA Machine 1 input")
  }

}