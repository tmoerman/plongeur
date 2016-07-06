package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors._
import org.apache.spark.mllib.stat.Statistics._
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
object Covering {

  /**
    * @param coveringBoundaries The boundaries in function of which to define the covering function.
    * @param lens The TDA Lens specification.
    * @param filterFunctions The reified filter functions.
    * @return Returns the CoveringFunction instance.
    */
  def levelSetsInverseFunction(coveringBoundaries: Boundaries,
                               lens: TDALens,
                               filterFunctions: Seq[FilterFunction]): LevelSetsInverseFunction = (p: DataPoint) => {

    val coveringIntervals =
      coveringBoundaries
        .zip(lens.filters)
        .zip(filterFunctions)
        .map { case (((min, max), filter), fn) => uniformCoveringIntervals(min, max, filter.nrBins, filter.overlap)(fn(p)) }

    combineToLevelSetIDs(coveringIntervals)
  }

  /**
    * @param filterFunctions The filter functions.
    * @param dataPoints The data RDD.
    * @return Returns an Array of Double tuples, representing the (min, max) boundaries of the filter functions applied
    *         on the RDD.
    */
  def calculateBoundaries(filterFunctions: Seq[FilterFunction],
                          dataPoints: RDD[DataPoint]): Array[(Double, Double)] = {

    val filterValues = dataPoints.map(p => dense(filterFunctions.toArray.map(f => f(p))))

    val stats = colStats(filterValues)

    stats.min.toArray zip stats.max.toArray
  }

  /**
    * @param boundaryMin The lower boundary of the filter function span.
    * @param boundaryMax The upper boundary of the filter function span.
    * @param nrBins The number of bins.
    * @param overlap The percentage of overlap between intervals.
    * @param x A filter function value.
    * @return Returns the lower coordinates of the intervals covering the specified filter value x.
    */
  def uniformCoveringIntervals(boundaryMin: BigDecimal,
                               boundaryMax: BigDecimal,
                               nrBins:      Int,
                               overlap:     Percentage)
                              (x: BigDecimal): Seq[BigDecimal] = {

    val binLength = getBinLength(boundaryMax - boundaryMin, nrBins, overlap)

    val increment = (1 - overlap) * binLength

    val diff = (x - boundaryMin) % increment
    val base = x - diff

    val q = binLength quot increment
    val r = binLength  %   increment

    val factor = if (r == 0) q - 1 else q

    val start = base - increment * factor
    val end   = base + increment

    Stream
      .continually(increment)
      .scanLeft(start)(_ + _)
      .takeWhile(_ < end)
  }

  /**
    * Derivation:
    *
    *   T = L + (n-1)*I   (I == increment length)
    *   I = L*(1-O)
    *
    *   T = L + (n-1)*L*(1-O)
    *     = L*(1 + (n-1)*(1-O))
    *     = L*(1 + (n-1) - (n-1)*O)
    *     = L*(n - (n-1)*O)
    *
    *   L = T / (n - (n-1)*O)
    *
    * @param T total length
    * @param n nr bins
    * @param O overlap percentage
    * @return Returns the bin length as a BigDecimal
    */
  def getBinLength(T: BigDecimal, n: Int, O: Percentage): BigDecimal = T / (n - (n-1)*O)

  /**
    * @param coveringIntervals The covering intervals corresponding to different filter functions.
    * @return Returns covering intervals in the individual dimensions combined to a LevelSetID.
    */
  def combineToLevelSetIDs[T](coveringIntervals: Seq[Seq[T]]): Set[Vector[T]] =
    coveringIntervals
      .foldLeft(Seq(Vector[T]())) {
        (acc, intervals) => intervals.flatMap(coordinate => acc.map(combos => combos :+ coordinate)) }
      .toSet

}
