package org.tmoerman.plongeur.test

import org.apache.commons.lang.StringUtils._
import org.apache.spark.mllib.linalg.Vectors._
import org.tmoerman.plongeur.tda.Model._

import scala.io.Source

/**
  * @author Thomas Moerman
  */
trait FileResources {

  val wd = "src/test/resources/data/"

  val heuristicFile = wd + "heuristic.csv"
  val test2dFile    = wd + "test.2d.csv"

  def parseToLabeledPoints(file: String) =
    Source
      .fromFile(file)
      .getLines
      .map(_.split(",").map(trim))
      .zipWithIndex
      .map{ case (Array(x, y), idx) => new IndexedDataPoint(idx, dense(x.toDouble, y.toDouble)) }
      .toList

  val heuristicData = parseToLabeledPoints(heuristicFile)

  val test2dData = parseToLabeledPoints(test2dFile)

}
