package org.tmoerman.plongeur.test

import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.regression.LabeledPoint
import org.tmoerman.plongeur.util.RDDFunctions._

import org.apache.commons.lang.StringUtils.trim

/**
  * @author Thomas Moerman
  */
trait TestResources extends SparkContextSpec with FileResources {

  val test2DFile    = wd + "test2D.csv"
  val irisFile      = wd + "iris.csv"

  val circle100 = wd + "circle.100.csv"
  val circle250 = wd + "circle.250.csv"
  val circle1k  = wd + "circle.1k.csv"
  val circle10k = wd + "circle.10k.csv"

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

  def readCircle(file: String) =
    sc
      .textFile(file)
      .map(_.split(",").map(trim))
      .sortBy{ case Array(x, y) => (x.toDouble,  y.toDouble) }
      .zipWithIndex()
      .map{ case (Array(x, y), idx) => LabeledPoint(idx, dense(x.toDouble, y.toDouble))}

  val circle100RDD = readCircle(circle100)
  val circle250RDD = readCircle(circle250)
  val circle1kRDD  = readCircle(circle1k)
  val circle10kRDD = readCircle(circle10k)

}