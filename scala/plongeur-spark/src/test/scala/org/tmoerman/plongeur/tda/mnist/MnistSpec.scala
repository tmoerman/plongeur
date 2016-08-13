package org.tmoerman.plongeur.tda.mnist

import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Colour.{AttributePredicate, Colouring, LocalPercentage}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.{Brewer, TDAMachine}
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.test.SparkContextSpec
import rx.lang.scala.Observable
import shapeless.HNil

import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
class MnistSpec extends FlatSpec with SparkContextSpec with Matchers {

  val wd = "src/test/resources/mnist/"

  val mnistTrainFile =  wd + "mnist_train.csv"

  def readMnist(file: String): RDD[DataPoint] =
    sc
      .textFile(file)
      .map(s => {
        val columns = s.split(",").map(trim).toList

        columns match {
          case cat :: rawFeatures =>
            val nonZero =
              rawFeatures.
                map(_.toInt).
                zipWithIndex.
                filter{ case (v, idx) => v != 0 }.
                map{ case (v, idx) => (idx, v.toDouble) }

            val sparseFeatures = Vectors.sparse(rawFeatures.size, nonZero)

            (cat, sparseFeatures)
        }})
      .zipWithIndex
      .map {case ((cat, features), idx) => dp(idx.toInt, features, Map("cat" -> cat))}

  "running a TDAMachine on mnist data with colouring" should "work" in {
    val mnistSampleRDD = readMnist(mnistTrainFile).sample(false, .05, 0l).cache

    val ctx = TDAContext(sc, mnistSampleRDD)

    val cat = AttributePredicate("cat", "0")

    val inParams =
      TDAParams(
        lens = TDALens(
          Filter("PCA" :: 0 :: HNil, 20, 0.33),
          Filter("PCA" :: 1 :: HNil, 20, 0.33)),
        clusteringParams = ClusteringParams(),
        collapseDuplicateClusters = false,
        colouring = Colouring(Brewer.palettes("Blues").get(9), LocalPercentage(9, cat)))

    val (outParams, result) =
      TDAMachine.run(ctx, Observable.just(inParams))
        .toBlocking
        .single

    println(result.clusters.flatMap(_.colours.headOption).toIterable.frequencies)
  }

}