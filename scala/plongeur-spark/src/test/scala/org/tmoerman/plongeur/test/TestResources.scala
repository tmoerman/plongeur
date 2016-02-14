package org.tmoerman.plongeur.test

import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.regression.LabeledPoint
import org.tmoerman.plongeur.util.RDDFunctions._

import org.apache.commons.lang.StringUtils.trim

/**
  * @author Thomas Moerman
  */
trait TestResources extends SparkContextSpec {

  val wd     = "src/test/resources/data/"
  val test2DFile    = wd + "test2D.csv"
  val irisFile      = wd + "iris.csv"
  val heuristicFile = wd + "heuristic.csv"

  lazy val test2DParsed =
    sc
      .textFile(test2DFile)
      .map(_.split(",").map(trim))
      .parseCsv{ case Array(a, b, c) => (a.toDouble, b.toDouble, c.toInt) }

  lazy val test2DLabeledPointsRDD =
    test2DParsed
      ._2
      .zipWithIndex
      .map{ case ((x, y, _), idx) => LabeledPoint(idx, dense(x, y)) }

  lazy val irisParsed =
    sc
      .textFile(irisFile)
      .map(_.split(",").map(trim))
      .parseCsv{ case Array(a, b, c, d, e) => (a.toDouble, b.toDouble, c.toDouble, d.toDouble, e) }

  lazy val heuristicLabeledPointsRDD =
    sc
      .textFile(heuristicFile)
      .map(_.split(",").map(trim))
      .zipWithIndex()
      .map{ case (Array(a, b), idx) => LabeledPoint(idx, dense(a.toDouble, b.toDouble)) }

}