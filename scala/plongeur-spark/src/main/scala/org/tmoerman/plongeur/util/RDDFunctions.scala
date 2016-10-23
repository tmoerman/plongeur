package org.tmoerman.plongeur.util

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model.DataPoint

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

  def distinctComboSets[O](orderingSelector: T => O)
                          (implicit ord: Ordering[O]): RDD[Set[T]] =
    (rdd cartesian rdd)
      .flatMap{ case ((e1, e2)) =>
        if (ord.lt(orderingSelector(e1), orderingSelector(e2))) Seq(Set(e1, e2)) else Nil
      }

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
    (rdd cartesian rdd)
      .filter{ case ((p1, p2)) => p1.index < p2.index }

  /**
    * @return Returns an RDD of all non-equal combination sets of the specified RDD.
    */
  def distinctComboSets: RDD[Set[DataPoint]] = new RDDFunctions(rdd).distinctComboSets(p => p.index)

}