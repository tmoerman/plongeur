package org.tmoerman.plongeur.spark.test

import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author Thomas Moerman
  */
object TestSparkContext {

  def create(appName:     String  = "Plongeur Test",
             master:      String  = "local[*]",
             memGigs:     Int     = 12,
             compressRDD: Boolean = true) = {

    val conf =
      new SparkConf()
        .setAppName(appName)
        .setMaster(master)
        .set("spark.rdd.compress", s"$compressRDD")
        .set("spark.driver.memory", memGigs + "g")
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    new SparkContext(conf)
  }

  lazy val instance = create()

}