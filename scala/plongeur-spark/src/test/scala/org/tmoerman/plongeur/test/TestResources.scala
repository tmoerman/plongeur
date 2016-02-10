package org.tmoerman.plongeur.test

import org.tmoerman.plongeur.util.RDDFunctions._

import org.apache.commons.lang.StringUtils.trim

/**
  * @author Thomas Moerman
  */
trait TestResources extends SparkContextSpec{

  val wd     = "src/test/resources/data/"
  val points = wd + "points.csv"
  val iris   = wd + "iris.csv"

  lazy val pointsRDD =
    sc
      .textFile(points)
      .map(_.split(",").map(trim))
      .parseCsv{ case Array(a, b, c) => (a.toDouble, b.toDouble, c.toInt) }

  lazy val irisRDD =
    sc
      .textFile(iris)
      .map(_.split(",").map(trim))
      .parseCsv{ case Array(a, b, c, d, e) => (a.toDouble, b.toDouble, c.toDouble, d.toDouble, e) }

}