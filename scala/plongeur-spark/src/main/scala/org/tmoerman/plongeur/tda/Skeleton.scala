package org.tmoerman.plongeur.tda

import org.apache.spark.Partitioner.defaultPartitioner
import org.apache.spark.mllib.linalg.Vectors.dense
import org.apache.spark.mllib.stat.Statistics.colStats
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance.{euclidean, DistanceFunction}

import org.tmoerman.plongeur.util.IterableFunctions._
import Clustering._
import Model._

/**
  * @author Thomas Moerman
  */
object Skeleton extends Serializable {

  /**
    * @param lens
    * @param data
    * @param boundaries
    * @param distanceFunction
    * @return
    */
  def execute(lens: Lens,
              data: RDD[DataPoint],
              boundaries: Option[Array[(Double, Double)]] = None,
              distanceFunction: DistanceFunction = euclidean): TDAResult = {

    val bounds = boundaries.getOrElse(calculateBoundaries(lens.functions, data))

    val covering = coveringFunction(lens, bounds)

    val clustersRDD: RDD[Cluster[Any]] =
      data
        .flatMap(p => covering(p).map(coords => (coords, p)))
        .repartitionAndSortWithinPartitions(defaultPartitioner(data)) // TODO which partitioner?
        .mapPartitions(_
          .view                           // TODO view ok here?
          .groupRepeats(selector = _._1)  // group by A
          .map(pairs => { val coords = pairs.head._1
                          val points = pairs.map(_._2)
                          cluster(points, coords, distanceFunction) }))   // cluster points
        .flatMap(_.map(c => (c.points, c)))   //
        .reduceByKey((c1, c2) => c1)          // collapse equivalent clusters
        .values
        .cache

    val edgesRDD: RDD[Set[Any]] =
      clustersRDD
        .flatMap(cluster => cluster.points.map(p => (p.index, cluster.id))) // melt all clusters by points
        .combineByKey((clusterId: Any) => Set(clusterId),  // TODO turn into groupByKey?
                      (acc: Set[Any], id: Any) => acc + id,
                      (acc1: Set[Any], acc2: Set[Any]) => acc1 ++ acc2)     // create proto-clusters, collapse doubles
        .values
        .flatMap(_.subsets(2))
        .distinct
        .cache

    TDAResult(bounds, clustersRDD, edgesRDD)
  }

  case class TDAResult(val bounds: Array[(Double, Double)],
                       val clustersRDD: RDD[Cluster[Any]],
                       val edgesRDD: RDD[Set[Any]]) extends Serializable {

    def clusters = clustersRDD.collect

    def edges = edgesRDD.collect

  }

  /**
    * @param filterFunctions The filter functions.
    * @param rdd The data RDD.
    * @return Returns an Array of Double tuples, representing the (min, max) boundaries of the filter functions applied
    *         on the RDD.
    */
  def calculateBoundaries(filterFunctions: Array[FilterFunction],
                          rdd: RDD[DataPoint]): Array[(Double, Double)] = {

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
                       boundaries: Array[(Double, Double)]): CoveringFunction = (p: DataPoint) =>
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
    * @return Returns covering intervals in the individual dimensions combined to a hyper cube coordinates vector.
    */
  def hyperCubeCoordinateVectors[BigDecimal](coveringIntervals: Seq[Seq[BigDecimal]]): Set[Vector[BigDecimal]] =
    coveringIntervals
      .foldLeft(Seq(Vector[BigDecimal]())) {
        (acc, intervals) => intervals.flatMap(coordinate => acc.map(combos => combos :+ coordinate)) }
      .toSet

}
