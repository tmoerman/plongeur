package org.tmoerman.plongeur.tda

import org.apache.spark.Partitioner
import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, RowMatrix}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.stat.Statistics.colStats
import org.apache.spark.rdd.RDD

/**
  * @author Thomas Moerman
  */
object Skeleton extends Serializable {
  import Model._

  def execute(lens: Lens,
              distanceFunction: DistanceFunction,
              rdd: RDD[LabeledPoint]) {

    val covering = coveringFunction(lens, rdd)

    val bla =
      rdd
        .flatMap(p => covering(p).map(k => (k, p)))
        .treeAggregate()

  }

  def coveringFunction(lens: Lens, rdd: RDD[LabeledPoint]): CoveringFunction = (p: LabeledPoint) =>
      hyperCubeCoordinates(
        filterBoundaries(lens.functions, rdd)
          .zip(lens.filters)
          .map { case ((min, max), filter) =>
            intersectingIntervals(min, max, filter.length, filter.overlap)(filter.function(p)) })

  def intersectingIntervals(min: BigDecimal,
                            max: BigDecimal,
                            length:  Percentage,
                            overlap: Percentage)
                           (x: BigDecimal): Seq[BigDecimal] = {

    val intervalLength = (max - min) * length

    val increment = (1 - overlap) * intervalLength

    val diff = (x - min) % increment
    val base = x - diff

    val q = intervalLength quot increment
    val r = intervalLength  %   increment

    val factor = if (r == 0) q - 1 else q

    val start = base - increment * factor
    val end   = base + increment

    Stream
      .continually(increment)
      .scanLeft(start)(_ + _)
      .takeWhile(_ < end)
  }

  def hyperCubeCoordinates(coveringValues: Seq[Seq[Any]]): Set[HyperCubeCoordinates] =
    coveringValues
      .foldLeft(Seq(Vector[Any]())) {
        (acc, intervals) => intervals.flatMap(coordinate => acc.map(combos => combos :+ coordinate)) }
      .toSet

  def filterBoundaries(functions: Array[FilterFunction],
                       rdd: RDD[LabeledPoint]): Array[(Double, Double)] = {

    val filterValues = rdd.map(p => dense(functions.map(f => f(p))))

    val stats = colStats(filterValues)

    stats.min.toArray zip stats.max.toArray
  }

}
