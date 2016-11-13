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

  implicit def pimpNumericRDD[K: ClassTag](rdd: RDD[(K, Double)]): NumericRDDFunctions[K] = new NumericRDDFunctions[K](rdd)

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

  def parseWithIndex[R](parseFn: (Long, Array[String]) => R,
                        cacheValues: Boolean = true)(implicit ev$1: R => Serializable, ev$2: ClassTag[R]) = {

    val header = rdd.first
    val values =
      rdd
        .drop(1)
        .zipWithIndex
        .map{ case (line, idx) => parseFn(idx, line) }

    (header, if (cacheValues) values.cache else values)
  }

  def parseCsv[R: ClassTag](parseFn: Array[String] => R = identity _,
                            cacheValues: Boolean = true) = {

    val header = rdd.first
    val values = rdd.drop(1).map(parseFn)

    (header, if (cacheValues) values.cache else values)
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

// TODO generalize to other numeric types
class NumericRDDFunctions[K: ClassTag](val rdd: RDD[(K, Double)]) extends Serializable {

  type ACC = (Double, Int)

  def init(v: Double) = (v, 1)

  def concat(acc: ACC, v: Double) = acc match { case (sum, count) => (sum + v, count + 1) }

  def merge(a: ACC, b: ACC) = (a, b) match { case ((s1, c1), (s2, c2)) => (s1 + s2, c1 + c2) }

  def averageByKey =
    rdd
      .combineByKey(init, concat, merge)
      .mapValues{ case (a, b) => a / b }

}