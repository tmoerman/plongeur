package org.tmoerman.plongeur.tda

import org.apache.spark.Partitioner._
import org.apache.spark.rdd.{CoGroupedRDD, RDD}
import org.tmoerman.plongeur.tda.Filters._
import org.tmoerman.plongeur.tda.Model._

/**
  * @author Thomas Moerman
  */
object Covering {

  /**
    * @param ctx
    * @param lens
    * @return
    */
  def levelSetInverseRDD(ctx: TDAContext, lens: TDALens): RDD[(LevelSetID, DataPoint)] = {
    val filterRDDs: List[FilterRDD] = for {
      filter  <- lens.filters
      key     <- toFilterKey(filter)
      factory <- ctx.filterCache.get(key) } yield factory.apply(filter.spec)

    val minMaxPerRDD = filterRDDs.map(boundaries)

    val pointsByIndex = ctx.dataPoints.keyBy(_.index)

    val coGrouped = new CoGroupedRDD(pointsByIndex :: filterRDDs, defaultPartitioner(pointsByIndex, filterRDDs: _*))

    coGrouped
      .flatMap{ case (index, it) => (it.map(_.head).toList: @unchecked) match {
        case p :: values =>
          val point = p.asInstanceOf[DataPoint]

          val filterValues = values.map(_.asInstanceOf[FilterValue])

          val coveringIntervals =
            (minMaxPerRDD, lens.filters, filterValues)
              .zipped
              .map{ case ((min, max), filter, v) => uniformCoveringIntervals(min, max, filter.nrBins, filter.overlap)(v) }

          combineToLevelSetIDs(coveringIntervals)
            .map(levelSetID => (levelSetID, point))}}
  }

  /**
    * @param boundaryMin The lower boundary of the filter function span.
    * @param boundaryMax The upper boundary of the filter function span.
    * @param nrBins The number of bins.
    * @param overlap The percentage of overlap between intervals.
    * @param image A filter function value.
    * @return Returns the lower coordinates of the intervals covering the specified filter value x.
    */
  def uniformCoveringIntervals(boundaryMin: BigDecimal,
                               boundaryMax: BigDecimal,
                               nrBins:      Int,
                               overlap:     Percentage)
                              (image: BigDecimal): Seq[BigDecimal] = {

    val binLength = getBinLength(boundaryMax - boundaryMin, nrBins, overlap)

    val increment = (1 - overlap) * binLength

    val diff = (image - boundaryMin) % increment
    val base = image - diff

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
