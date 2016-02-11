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

  implicit def myOrdering[T <: Ordering[T]]: Ordering[Vector[Any]] = ??? // TODO fix

  def execute(lens: Lens,
              distanceFunction: DistanceFunction,
              rdd: RDD[LabeledPoint]) {

    val boundaries = calculateBoundaries(lens.functions, rdd)

    val covering = coveringFunction(lens, boundaries)

    val bla =
      rdd
        .flatMap(p => covering(p).map(k => (k, p))) // RDD[(HypercubeCoordinate, LabeledPoint)]

        //.repartitionAndSortWithinPartitions(rdd.partitioner.get) // TODO which partitioner?

  }

  /**
    * @param filterFunctions The filter functions.
    * @param rdd The data RDD.
    * @return Returns an Array of Double tuples, representing the (min, max) boundaries of the filter functions applied
    *         on the RDD.
    */
  def calculateBoundaries(filterFunctions: Array[FilterFunction],
                          rdd: RDD[LabeledPoint]): Array[(Double, Double)] = {

    val filterValues = rdd.map(p => dense(filterFunctions.map(f => f(p))))

    val stats = colStats(filterValues)

    stats.min.toArray zip stats.max.toArray
  }

  /**
    * @param lens The TDA Lens specification.
    * @param boundaries The boundaries in function of which to define the covering function.
    * @return Returns the CoveringFunction instance.
    */
  def coveringFunction(lens: Lens,
                       boundaries: Array[(Double, Double)]): CoveringFunction = (p: LabeledPoint) =>
    hyperCubeCoordinateVectors(
      boundaries
        .zip(lens.filters)
        .map { case ((min, max), filter) =>
          intersectingIntervals(min, max, filter.length, filter.overlap)(filter.function(p)) })

  /**
    * @param min
    * @param max
    * @param length
    * @param overlap
    * @param x
    * @return Returns the
    */
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

  def hyperCubeCoordinateVectors(coveringValues: Seq[Seq[Any]]): Set[HyperCubeCoordinateVector] =
    coveringValues
      .foldLeft(Seq(Vector[Any]())) {
        (acc, intervals) => intervals.flatMap(coordinate => acc.map(combos => combos :+ coordinate)) }
      .toSet

}
