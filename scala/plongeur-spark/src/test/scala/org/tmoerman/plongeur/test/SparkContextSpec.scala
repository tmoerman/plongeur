package org.tmoerman.plongeur.test

import org.tmoerman.plongeur.spark.test.TestSparkContext

/**
  * @author Thomas Moerman
  */
trait SparkContextSpec {

  lazy val sc = TestSparkContext.instance

}
