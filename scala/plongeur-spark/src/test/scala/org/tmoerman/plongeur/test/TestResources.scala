package org.tmoerman.plongeur.test

import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vectors._
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * @author Thomas Moerman
  */
trait TestResources extends SparkContextSpec with FileResources {

  val test2DFile = wd + "test2D.csv"
  val irisFile   = wd + "iris.csv"

  val circle100 = wd + "circle.100.csv"
  val circle250 = wd + "circle.250.csv"
  val circle1k  = wd + "circle.1k.csv"
  val circle10k = wd + "circle.10k.csv"

  lazy val test2DParsed =
    sc
      .textFile(test2DFile)
      .map(_.split(",").map(trim))
      .parseCsv{ case Array(a, b, c) => (a.toDouble, b.toDouble, c.toInt) }

  lazy val test2DLabeledPointsRDD: RDD[DataPoint] =
    test2DParsed
      ._2
      .zipWithIndex
      .map{ case ((x, y, _), idx) => dp(idx, dense(x, y)) }

  lazy val irisParsed =
    sc
      .textFile(irisFile)
      .map(_.split(",").map(trim))
      .parseCsv{ case Array(a, b, c, d, e) => (a.toDouble, b.toDouble, c.toDouble, d.toDouble, e) }

  lazy val irisDataPointsRDD: RDD[DataPoint] =
    irisParsed
      ._2
      .zipWithIndex
      .map{ case ((a, b, c, d, cat), idx) => DataPoint(idx.toInt, dense(Array(a, b, c, d)), Some(Map("cat" -> cat))) }

  lazy val heuristicLabeledPointsRDD: RDD[DataPoint] =
    sc
      .textFile(heuristicFile)
      .map(_.split(",").map(trim))
      .zipWithIndex
      .map{ case (arr, idx) => dp(idx, dense(arr.map(_.toDouble))) }

  def readDense(file: String): RDD[DataPoint] =
    sc
      .textFile(file)
      .map(_.split(",").map(trim))
      .sortBy{ case Array(x, y) => (x.toDouble,  y.toDouble) }
      .zipWithIndex
      .map{ case (arr, idx) => dp(idx, dense(arr.map(_.toDouble))) }

  def readMnist(file: String) =
    sc
      .textFile(file)
      .map(s => {
        val columns = s.split(",").map(trim).toList

        (columns: @unchecked) match {
          case cat :: rawFeatures =>
            val nonZero =
              rawFeatures
                .map(_.toInt)
                .zipWithIndex
                .filter{ case (v, idx) => v != 0 }
                .map{ case (v, idx) => (idx, v.toDouble) }

            val sparseFeatures = Vectors.sparse(rawFeatures.size, nonZero)

            (cat, sparseFeatures) }})
      .zipWithIndex
      .map {case ((cat, features), idx) => DataPoint(idx.toInt, features, Some(Map("cat" -> cat)))}

  lazy val circle100RDD = readDense(circle100)
  lazy val circle250RDD = readDense(circle250)
  lazy val circle1kRDD  = readDense(circle1k)
  lazy val circle10kRDD = readDense(circle10k)

}