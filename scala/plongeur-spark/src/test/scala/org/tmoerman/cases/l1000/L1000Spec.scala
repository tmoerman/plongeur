package org.tmoerman.cases.l1000

import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.LpNormDistance
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model.{DataPoint, TDAContext, dp}
import org.tmoerman.plongeur.tda.knn.FastKNN_BAK.FastKNNParams
import org.tmoerman.plongeur.tda.knn.SampledKNN.SampledKNNParams
import org.tmoerman.plongeur.tda.knn.{FastKNN, SampledKNN, _}
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

  val perts = L1000Reader.read(geneXPSignatures)(sc)._2

  val s = perts.count.toInt

  val fastKNNParams = {
    val k   = 10
    val B   = 50
    val sig = 10
    val L   = 1
    val dist = LpNormDistance(0.5)
    val r = None
    val lshParams = LSHParams(signatureLength = sig, radius = r, distance = dist)

    FastKNNParams(k = k, blockSize = B, nrHashTables = L, lshParams = lshParams)
  }

  import fastKNNParams._
  import lshParams._

  it should "compute a series of runs" in {
    // val (pctTotal, sampleSize) = (1.0, Right(0.01))
    val (pctTotal, sampleSize) = (0.25, Right(0.10))
    // val (pctTotal, sampleSize) = (0.1, Right(0.25))

    val ctx = TDAContext(sc, if (pctTotal < 1.0) perts.sample(false, pctTotal) else perts)

    val sampledKNNParams = SampledKNNParams(k = k, sampleSize = sampleSize, distance = distance)

    val baseLine = SampledKNN.apply(ctx, sampledKNNParams).cache

    val radius = Some(10.0)

    //Stream(1, 5, 10, 15, 20, 25) // , 30, 50) // TODO write this more efficiently with a scan algorithm
    //Option(90)
    Stream(10, 30, 60)
      .map(L => fastKNNParams.copy(nrHashTables = L, lshParams = lshParams.copy(radius = radius)))
      .map(p => run(ctx, pctTotal, sampleSize, p, baseLine))
      .foreach(println)
  }

  def run(ctx: TDAContext, pctTotal: Double, sample: Either[Int, Double], fastKNNParams: FastKNNParams, baseLine: KNN_RDD) = {
    import fastKNNParams._
    import lshParams._

    val ((result, accuracy), wallTime) = time {
      val rdd = FastKNN(ctx, fastKNNParams).cache

      (rdd, relativeAccuracy(rdd, baseLine))
    }

    val comment = "MurmurHash, preserve = false"
    val now = DateTime.now

    (distance, k, radius, signatureLength, nrHashTables, blockSize, pctTotal, wallTime.toSeconds, accuracy, sample)

    s"| k(NN)=$k | $distance | ${radius.map(v => s"r=$v").getOrElse("N/A")} | sig=$signatureLength | L=$nrHashTables | B=$blockSize | $pctTotal | ${wallTime.toSeconds}s | ${f"$accuracy%1.3f"} | $sample | $now | $comment | "
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