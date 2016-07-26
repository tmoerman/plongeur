package org.tmoerman.plongeur.util

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance.DistanceFunction
import org.tmoerman.plongeur.tda.Model
import org.tmoerman.plongeur.tda.Model.{Index, DataPoint}

import scala.reflect.ClassTag

/**
  * @author Thomas Moerman
  */
object RDDFunctions {

  implicit def pimpRDD[T: ClassTag](rdd: RDD[T]): RDDFunctions[T] = new RDDFunctions[T](rdd)

  implicit def pimpCsvRDD(rdd: RDD[Array[String]]): CsvRDDFunctions = new CsvRDDFunctions(rdd)

  implicit def pimpDataPointRDD(rdd: RDD[DataPoint]): DataPointRDDFunctions = new DataPointRDDFunctions(rdd)

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

class DataPointRDDFunctions(val rdd: RDD[DataPoint]) extends Serializable {

  /**
    * @return Returns an RDD of all non-equal combination pairs of the specified RDD.
    */
  def distinctComboPairs: RDD[(DataPoint, DataPoint)] =
    rdd
      .cartesian(rdd)
      .filter{ case ((p1, p2)) => p1.index < p2.index }

  /**
    * @return Returns an RDD of all non-equal combination sets of the specified RDD.
    */
  def distinctComboSets: RDD[Set[DataPoint]] =
    rdd
      .cartesian(rdd)
      .flatMap{ case ((p1, p2)) => if (p1.index < p2.index) List(Set(p1, p2)) else Nil }

  /**
    * @param distance
    * @return Returns an RDD of DataPoint pairs to
    */
  def distanceMatrix(distance: DistanceFunction): RDD[(Set[Index], Double)] =
    distinctComboPairs
      .map{ case ((p1, p2)) => (Set(p1.index, p2.index), distance.apply(p1, p2)) }

}