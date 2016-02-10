package org.tmoerman.plongeur.util

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
  * @author Thomas Moerman
  */
object RDDFunctions {

  implicit def pimpRDD[T: ClassTag](rdd: RDD[T]): RDDFunctions[T] = new RDDFunctions[T](rdd)

  implicit def pimpCsvRDD(rdd: RDD[Array[String]]): CsvRDDFunctions = new CsvRDDFunctions(rdd)

}

class RDDFunctions[T: ClassTag](val rdd: RDD[T]) extends Serializable {

  def drop(n: Int) = rdd.mapPartitionsWithIndex{ (idx, it) => if (idx == 0) it.drop(n) else it }

}

class CsvRDDFunctions(val rdd: RDD[Array[String]]) extends Serializable {
  import RDDFunctions._

  def parseCsv[R: ClassTag](parseFn: Array[String] => R = identity _,
                            cacheValues: Boolean = true) = {

    val header = rdd.first()
    val values = rdd.drop(1).map(parseFn)

    (header, if (cacheValues) values.cache() else values)
  }

}