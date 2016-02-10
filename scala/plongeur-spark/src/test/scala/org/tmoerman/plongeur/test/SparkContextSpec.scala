package org.tmoerman.plongeur.test

import org.apache.spark.sql.SQLContext
import org.tmoerman.plongeur.spark.test.TestSparkContext

/**
  * @author Thomas Moerman
  */
trait SparkContextSpec {

  implicit lazy val sc = TestSparkContext.instance

  implicit lazy val sql = new SQLContext(sc)

}
