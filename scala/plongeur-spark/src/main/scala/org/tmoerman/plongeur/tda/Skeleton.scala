package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, RowMatrix}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.stat.Statistics.colStats
import org.apache.spark.rdd.RDD

import scala.annotation.tailrec

/**
  * @author Thomas Moerman
  */
object Skeleton extends Serializable {
  import Model._

  def execute {



  }

  def coverageFunction(lens: Lens,
                       rdd: RDD[LabeledPoint]): CoverageFunction = {

    val result =
      (filterBoundaries(lens.functions, rdd) zip lens.functions)
        .map { case ((min, max), f) =>
          ???
        }

    (p: LabeledPoint) => ???
  }

  def intersectingIntervals(min: BigDecimal,
                            max: BigDecimal,
                            pctLength:  BigDecimal,
                            pctOverlap: BigDecimal)
                           (x: BigDecimal): Seq[BigDecimal] = {

    if (x == min) return Seq(min)

    val length = (max - min) * pctLength

    val increment = (1 - pctOverlap) * length

    val diff = (x - min) % increment
    val base = x - diff

    val q = length quot increment
    val r = length % increment

    val factorL = if (r == 0) q - 1 else q

    val start = base - increment * factorL
    val end   = base + increment

    Stream
      .continually(increment)
      .scanLeft(start)(_ + _)
      .takeWhile(_ < end)
  }

  def combineCoordinates[A](coveringValues: List[List[A]]) = {

    @tailrec
    def recur(acc: List[Vector[A]],
              values: List[List[A]]): List[Vector[A]] = values match {
      case Nil => acc
      case x :: xs => recur(x.flatMap(v => acc.map(combos => combos :+ v)), xs)
    }

    recur(List(Vector[A]()), coveringValues)
  }

  def filterBoundaries(functions: Array[FilterFunction],
                       rdd: RDD[LabeledPoint]): Array[(Double, Double)] = {

    val filterValues = rdd.map(p => dense(functions.map(f => f(p))))

    val stats = colStats(filterValues)

    stats.min.toArray zip stats.max.toArray
  }

}
