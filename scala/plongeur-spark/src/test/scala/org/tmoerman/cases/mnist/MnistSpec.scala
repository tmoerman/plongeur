package org.tmoerman.cases.mnist

import com.holdenkarau.spark.testing.SharedSparkContext
import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Brewer.palettes
import org.tmoerman.plongeur.tda.Colour._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.TDAMachine
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.util.IterableFunctions._
import rx.lang.scala.Observable

/**
  * @author Thomas Moerman
  */
class MnistSpec extends FlatSpec with SharedSparkContext with Matchers {

  val wd = "src/test/resources/mnist/"

  val mnistTrainFile = wd + "mnist_train.csv"

  def readMnist(file: String): RDD[DataPoint] =
    sc
      .textFile(file)
      .map(s => {
        val columns = s.split(",").map(trim).toList

        (columns: @unchecked) match {
          case cat :: rawFeatures =>
            val nonZero =
              rawFeatures.
                map(_.toInt).
                zipWithIndex.
                filter { case (v, idx) => v != 0 }.
                map { case (v, idx) => (idx, v.toDouble) }

            val sparseFeatures = Vectors.sparse(rawFeatures.size, nonZero)

            (cat, sparseFeatures)
        }
      })
      .zipWithIndex
      .map { case ((cat, features), idx) => dp(idx.toInt, features, Map("cat" -> cat)) }

  "running a TDAMachine on mnist data with colouring" should "work" in {
    val mnistSampleRDD = readMnist(mnistTrainFile).sample(false, .05, 0l).cache

    val ctx = TDAContext(sc, mnistSampleRDD)

    val zero = (d: DataPoint) => d.meta.get("cat") == "0"

    val inParams =
      TDAParams(
        lens = TDALens(
          Filter(PrincipalComponent(0), 20, 0.33),
          Filter(PrincipalComponent(1), 20, 0.33)),
        clusteringParams = ClusteringParams(),
        collapseDuplicateClusters = false,
        colouring = ClusterPercentage(palettes("Set3")(10), zero))

    val result =
      TDAMachine.run(ctx, Observable.just(inParams))
        .toBlocking
        .single

    println(result.clusters.flatMap(_.colours.headOption).toIterable.frequencies)

    Helper.printInspections(result, "Mnist")
  }
}

object Helper {

  import org.tmoerman.plongeur.tda.Inspections._

  implicit val counter = mapToInt

  def printInspections(result: TDAResult, name: String) = {
    println(
      Seq(result.levelSetsToClusters.mkString("\n"),
        result.pointsToClusters.mkString("\n"),
        result.dotGraph(name))
        .mkString("\n"))
  }

}