package org.tmoerman.cases.l1000

import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.rdd.RDD
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.CosineDistance
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model.{DataPoint, TDAContext, dp}
import org.tmoerman.plongeur.tda.knn.FastKNN.FastKNNParams
import org.tmoerman.plongeur.tda.knn.SampledKNN.SampledKNNParams
import org.tmoerman.plongeur.tda.knn.{KNN, FastKNN, SampledKNN}
import org.tmoerman.plongeur.test.SparkContextSpec
import org.tmoerman.plongeur.util.RDDFunctions._
import org.tmoerman.plongeur.util.TimeUtils.time

/**
  * @author Thomas Moerman
  */
class L1000Spec extends FlatSpec with SparkContextSpec with Matchers {

  val wd = "src/test/resources/l1000/"

  val geneXPSignatures =  wd + "LINCS_Gene_Expression_signatures_CD.csv"

  behavior of "L1000"

  val perts = L1000Reader.read(geneXPSignatures)._2.sample(false, 0.1)

  val ctx = TDAContext(sc, perts)

  it should "pass smoke test" ignore {
    val size = perts.count

    println(size) // 20339

    val top3 = perts.take(3)

    println(top3.map(pert => (pert.meta, pert.features.size)).mkString("\n"))
  }

  it should "be possible to compute an approximate kNN matrix" in {
    val kNN = 10
    val B   = 200
    val sig = 20
    val L   = 1
    val sample = Right(0.25)

    val lshParams = LSHParams(signatureLength = sig, radius = None, distance = CosineDistance)

    val fastKNNParams = FastKNNParams(k = kNN, blockSize = B, nrHashTables = L, lshParams = lshParams)

    val (fastACC, fastDuration) = time { FastKNN.fastACC(ctx, fastKNNParams) }

    val sampledKNNParams = SampledKNNParams(k = kNN, sampleSize = sample, distance = CosineDistance)

    val (sampledACC, sampledDuration) = time{ SampledKNN.sampledACC(ctx, sampledKNNParams) }

    val accuracy = KNN.accuracy(fastACC, sampledACC)

    println(s"fastACC computed in ${fastDuration.toSeconds}s with accuracy $accuracy, sampledACC computed in ${sampledDuration}s")
  }

}

object L1000Reader extends Serializable {

  def read(file: String)(implicit sc: SparkContext): (Array[String], RDD[DataPoint]) = {

    def parseLine(index: Long, cols: Array[String]) =
      dp(
        index,
        dense(cols.tail.map(_.toDouble)),
        Map("pertID" -> cols.head))

    sc
      .textFile(file)
      .map(line => line.split(",").map(trim))
      .parseWithIndex(parseLine)
  }

}