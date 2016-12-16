package org.tmoerman.cases.ghb

import com.holdenkarau.spark.testing.SharedSparkContext
import org.apache.commons.lang.StringUtils._
import org.apache.spark.mllib.linalg.Vectors
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model.dp
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * @author Thomas Moerman
  */
class ScAtacSpec extends FlatSpec with SharedSparkContext with Matchers {

  val wd = "/Users/tmo/Work/ghb2016/data/scATAC/"

  val alleles_normVars    = wd + "Alleles_normVars_Motifs_v7_noBadTFPro_w_Names.tsv"
  val alleles_shuffled    = wd + "Alleles_shuffled_normDevMat_Motifs_v7_noBadTFPro_w_Names.tsv"
  val inferAlleles        = wd + "inferAllels_matrix_OptixRSELPeaks_RSL_RSE_Optix.csv"
  val inferAlleles_SimPos = wd + "inferAllels_matrix_OptixRSELPeaks_RSL_RSE_Optix_SimPos.csv"
  val peakAssoc           = wd + "peakAssoc_Motifs_v7_noBadTFPro_w_Labels.csv"
  val singles             = wd + "singlesPeaks_Alleles_Motifs_v7_noBadTFPro_w_Labels.csv"

  val labels  = wd + "cell_labels.csv"

  "parsing the singles file" should "work" in {
    val raw = sc.textFile(singles)

    println(s"singles.count == ${raw.count}")

    val count = raw.count

    val (header, rdd) =
      raw
        .map(line => line.split(",").map(trim))
        .parseWithIndex(ScAtacReader.parseSparseFeatures)

    val top3 = rdd.take(3)

    top3.toString


  }

//  "parsing the peakAssoc file to binary sparse vectors" should "work" ignore {
//    val raw = sc.textFile(peakAssoc)
//    val count = raw.count
//    val (header, rdd) =
//      raw
//        .map(line => line.split(",").map(trim))
//        .parseWithIndex(ScAtacReader.parseSparseFeatures)
//    val top3 = rdd.take(3)
//    top3.toString
//  }

}

object ScAtacReader {

  def parseSingles(index: Long, cols: Array[String]) = ???

  def parseSparseFeatures(index: Long, cols: Array[String]) = cols.toList match {
    case head :: rest =>
      val nonZeroByIndex =
        rest
          .zipWithIndex
          .flatMap{ case (s, i) => if (s == "0") Nil else (i, s.toDouble) :: Nil }

      val featureSparse = Vectors.sparse(rest.size, nonZeroByIndex)

      dp(index, featureSparse, Map("id" -> head))
  }

}