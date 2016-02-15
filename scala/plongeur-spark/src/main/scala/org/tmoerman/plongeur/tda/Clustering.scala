package org.tmoerman.plongeur.tda

import java.util.UUID

import org.apache.spark.mllib.regression.LabeledPoint
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, euclidean}
import org.tmoerman.plongeur.util.IterableFunctions._

import smile.clustering.HierarchicalClustering
import smile.clustering.linkage._

import scalaz.Memo._

/**
  * Recycled a few methods from smile-scala, which is not released as a Maven artifact.
  *
  * @author Thomas Moerman
  */
object Clustering extends Serializable {

  type DistanceMatrix = Array[Array[Double]]

  case class Cluster[ID](val id: ID,
                         val points: Set[LabeledPoint]) extends Serializable {

    lazy val labels = points.map(_.label)

    /**
      * @param that a Cluster.
      * @return Returns whether any point is common to both clusters.
      */
    @Deprecated // this would require a Cartesian combination of 2 RDDs of clusters -> Maybe test this strategy later.
    def intersects(that: Cluster[ID]) = {
      val (small, large) = // some optimization going on here
        if (this.points.size < that.points.size)
          (this.labels, that.labels) else
          (that.labels, this.labels)

      small.exists(e => large.contains(e))
    }

    override def toString = s"Cluster($id)"

    def verbose = s"""Cluster($id, $points)"""

  }

  def linkage(method: String, distanceMatrix: DistanceMatrix) = method match {
    case "single"   => new SingleLinkage(distanceMatrix)
    case "complete" => new CompleteLinkage(distanceMatrix)
    case "upgma"    => new UPGMALinkage(distanceMatrix)
    case "upgmc"    => new UPGMCLinkage(distanceMatrix)
    case "wpgma"    => new WPGMALinkage(distanceMatrix)
    case "wpgmc"    => new WPGMCLinkage(distanceMatrix)
    case "ward"     => new WardLinkage(distanceMatrix)

    case _ => throw new IllegalArgumentException(s"Unknown agglomeration method: $method")
  }

  def distances(data: Seq[LabeledPoint], distance: DistanceFunction): DistanceMatrix = {
    val n = data.length
    val result = new Array[Array[Double]](n)

    for (i <- 0 until n) {
      result(i) = new Array[Double](i+1)
      for (j <- 0 until i) {
        result(i)(j) = distance(data(i), data(j))
      }
    }

    result
  }

  type Heights = Iterable[Double]

  type PartitionHeuristic = (Heights) => Double

  /**
    * @param k Resolution.
    * @return Returns the cutoff height for partitioning a Hierarchical clustering.
    */
  def histogramPartitionHeuristic(k: Int = 10) = (heights: Heights) => heights.toList match {
    case Nil      => 0
    case x :: Nil => 0
    case        _ =>
      val inc = (heights.max - heights.min) / k

      val frequencies = heights.map(d => (BigDecimal(d) quot inc).toInt).frequencies

      (frequencies.keys.min to frequencies.keys.max)
        .dropWhile(x => frequencies(x) != 0)
        .headOption
        .getOrElse(1) * inc
  }

  type ClusterIdentifier[ID] = (Int) => ID

  def uuidClusterIdentifier: ClusterIdentifier[UUID] = mutableHashMapMemo(_ => UUID.randomUUID)

  val SINGLE_LINKAGE = "single"

  /**
    * @param data The LabeledPoint instances to cluster.
    * @param distanceFunction The distance function.
    * @param method The hierarchical clustering method: single, complete, etc. Default = "single".
    * @param partitionHeuristic The hierarchical clustering cutoff heuristic.
    * @param clusterIdentifier The function that turns cluster labels into global identifiers.
    *
    * @return The List of Clusters.
    */
  def cluster(data: Seq[LabeledPoint],
              distanceFunction: DistanceFunction = euclidean,
              method: String = SINGLE_LINKAGE,
              partitionHeuristic: PartitionHeuristic = histogramPartitionHeuristic(),
              clusterIdentifier: ClusterIdentifier[Any] = uuidClusterIdentifier): List[Cluster[Any]] =
    makeClusters(
      data,
      clusterLabels(data, distanceFunction, method, partitionHeuristic),
      clusterIdentifier)


  /**
    * @param data The LabeledPoint instances to cluster.
    * @param distanceFunction The distance function.
    * @param method The hierarchical clustering method: single, complete, etc. Default = "single".
    * @param partitionHeuristic The hierarchical clustering cutoff heuristic.
    *
    * @return Returns an Array of Ints that represent for each LabeledPoint in the data, to which cluster it belongs.
    */
  def clusterLabels(data: Seq[LabeledPoint],
                    distanceFunction: DistanceFunction = euclidean,
                    method: String = SINGLE_LINKAGE,
                    partitionHeuristic: PartitionHeuristic = histogramPartitionHeuristic()): Array[Int] =
    data.size match {
      case 0 => throw new scala.IllegalArgumentException("data size cannot be 0")
      case 1 => Array(0)
      case _ =>
        val distanceMatrix = distances(data, distanceFunction)

        val hierarchicalClustering = new HierarchicalClustering(linkage(method, distanceMatrix))

        val cutoffHeight = partitionHeuristic(hierarchicalClustering.getHeight)

        hierarchicalClustering.partition(cutoffHeight)
    }


  private def makeClusters(data: Seq[LabeledPoint],
                   clusterLabels: Array[Int],
                   clusterIdentifier: ClusterIdentifier[Any]): List[Cluster[Any]] =
    clusterLabels
      .zipWithIndex
      .map{ case (clusterLabel, pointIdx) => (clusterLabel, data(pointIdx)) }
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map{ case (clusterLabel, points) => Cluster(clusterIdentifier(clusterLabel), points.toSet) }
      .toList

}
