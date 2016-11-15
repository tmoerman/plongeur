package org.tmoerman.cases

import org.apache.commons.lang.StringUtils.trim
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
package object mixture {

  def readMixture(file: String)(sc: SparkContext): RDD[DataPoint] =
    sc
      .textFile(file)
      .zipWithIndex
      .map{ case (line, idx) =>
        val columns = line.split(",").map(trim)
        val category = columns.head
        val features = columns.tail.map(_.toDouble)

        dp(idx, Vectors.dense(features), Map("cat" -> category)) }

}