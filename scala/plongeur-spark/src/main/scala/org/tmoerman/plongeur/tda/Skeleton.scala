package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.stat.Statistics.colStats
import org.apache.spark.rdd.RDD

/**
  * @author Thomas Moerman
  */
object Skeleton extends Serializable {
  import Model._

  //implicit def myOrdering[T <: Ordering[T]]: Ordering[Vector[Any]] = ??? // TODO fix

  def execute(lens: Lens,
              distanceFunction: DistanceFunction,
              rdd: RDD[LabeledPoint]) {

//    val tuningRDD = rdd.first.toSeq.zipWithIndex.map(_.swap))
//    val nrPartitions: Int = rdd.getNumPartitions
//    val partitioner = new RangePartitioner(nrPartitions, tuningRDD)

    val boundaries = calculateBoundaries(lens.functions, rdd)

    val covering = coveringFunction(lens, boundaries)

    val bla =
      rdd
        .flatMap(p => covering(p).map(hcc => (hcc, p))) // RDD[(HypercubeCoordinate, LabeledPoint)]

        //.rep(rdd.partitioner.get) // TODO which partitioner?
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
          coveringIntervals(min, max, filter.length, filter.overlap)(filter.function(p)) })

  /**
    * @param boundaryMin The lower boundary of the filter function span.
    * @param boundaryMax The upper boundary of the filter function span.
    * @param lengthPct The percentage of length of the filter function span for an interval.
    * @param overlapPct The percentage of overlap between intervals.
    * @param x A filter function value.
    * @return Returns the lower coordinates of the intervals covering the specified filter value x.
    */
  def coveringIntervals(boundaryMin: BigDecimal,
                        boundaryMax: BigDecimal,
                        lengthPct:   Percentage,
                        overlapPct:  Percentage)
                       (x: BigDecimal): Seq[BigDecimal] = {

    val intervalLength = (boundaryMax - boundaryMin) * lengthPct

    val increment = (1 - overlapPct) * intervalLength

    val diff = (x - boundaryMin) % increment
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

  /**
    * @param coveringIntervals The covering intervals corresponding to different filter functions.
    * @return Combines the covering intervals in the individual dimensions to a hyper cube coordinates vector.
    */
  def hyperCubeCoordinateVectors(coveringIntervals: Seq[Seq[Any]]): Set[HyperCubeCoordinateVector] =
    coveringIntervals
      .foldLeft(Seq(Vector[Any]())) {
        (acc, intervals) => intervals.flatMap(coordinate => acc.map(combos => combos :+ coordinate)) }
      .toSet

}
