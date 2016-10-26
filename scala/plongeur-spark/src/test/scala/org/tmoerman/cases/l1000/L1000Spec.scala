package org.tmoerman.cases.l1000

import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.{ManhattanDistance, CosineDistance}
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model.{DataPoint, TDAContext, dp}
import org.tmoerman.plongeur.tda.knn.FastKNN.FastKNNParams
import org.tmoerman.plongeur.tda.knn.KNN.{kNN_RDD, ACC}
import org.tmoerman.plongeur.tda.knn.SampledKNN.SampledKNNParams
import org.tmoerman.plongeur.tda.knn.{FastKNN2, FastKNN, KNN, SampledKNN}
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

  val perts = L1000Reader.read(geneXPSignatures)._2

  val s = perts.count.toInt

  val fastKNNParams = {
    val k   = 10
    val B   = 200
    val sig = 40
    val L   = 1
    val dist = CosineDistance
    val r = None
    val lshParams = LSHParams(signatureLength = sig, radius = r, distance = dist)

    FastKNNParams(k = k, blockSize = B, nrHashTables = L, lshParams = lshParams)
  }

  import fastKNNParams._
  import lshParams._

  it should "compute an approximate kNN matrix and its accuracy" in {
    val pctTotal  = 1.0
    val pctSample = 0.025

    // val summary = run(pctTotal, pctSample, fastKNNParams)
    // println(summary)
  }

  it should "compute a series of runs" in {
    val pctTotal  = 0.1
    val pctSample = 0.25

    val ctx = TDAContext(sc, if (pctTotal < 1.0) perts.sample(false, pctTotal) else perts)

    val sampledKNNParams = SampledKNNParams(k = k, sampleSize = Right(pctSample), distance = distance)

    val baseLine = SampledKNN.apply(ctx, sampledKNNParams)

    //Stream(1, 5, 10, 15, 20, 25) // , 30, 50) // TODO write this more efficiently with a scan algorithm
    Option(30)
      .map(L => fastKNNParams.copy(nrHashTables = L, lshParams = lshParams.copy(radius = Some(LSH.estimateRadius(ctx)))))
      .map(p => run(ctx, pctTotal, pctSample, p, baseLine))
      .foreach(println)
  }

  def run(ctx: TDAContext, pctTotal: Double, pctSample: Double, fastKNNParams: FastKNNParams, baseLine: kNN_RDD): String = {
    import fastKNNParams._
    import lshParams._

    //val (rdd, fastDuration) = time { FastKNN(ctx, fastKNNParams) }
    val ((rdd, _), fastDuration) = time {
      val rdd = FastKNN2(ctx, fastKNNParams).cache

      val matrix = KNN.toSparseMatrix(s, rdd)

      (rdd, matrix)
    }

    val accuracy = KNN.accuracy(rdd, baseLine)

    val now = DateTime.now

    s"| $k | $distance | $signatureLength | $nrHashTables | $blockSize | $pctTotal | ${fastDuration.toSeconds}s | ${f"$accuracy%1.3f"} | $pctSample | $now | | "
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