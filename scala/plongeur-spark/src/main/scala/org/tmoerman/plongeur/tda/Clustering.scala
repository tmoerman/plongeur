package org.tmoerman.plongeur.tda

import java.util.UUID

import org.apache.spark.mllib.regression.LabeledPoint
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, euclidean}
import org.tmoerman.plongeur.tda.Model.HyperCubeCoordinateVector
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

  case class Cluster[ID](val id: ID,
                         val coords: HyperCubeCoordinateVector,
                         val points: Set[LabeledPoint]) extends Serializable {

    def size = points.size

    def verbose = s"""Cluster($id, $points)"""

    override def toString = s"Cluster($id)"

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

  type DistanceMatrix = Array[Array[Double]]

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
  def histogramPartitionHeuristic(k: Int = 100) = (heights: Heights) => heights.toList match {
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

  private val SINGLE_LINKAGE = "single"

  /**
    * @param data The LabeledPoint instances to cluster.
    * @param distanceFunction The distance function.
    * @param method The hierarchical clustering method: single, complete, etc. Default = "single".
    * @param partitionHeuristic The hierarchical clustering cutoff heuristic.
    * @param clusterIdentifier The function that turns cluster labels into global identifiers.
    * @return The List of Clusters.
    */
  def cluster(data: Seq[LabeledPoint],
              coords: HyperCubeCoordinateVector = Vector(),
              distanceFunction: DistanceFunction = euclidean,
              method: String = SINGLE_LINKAGE,
              partitionHeuristic: PartitionHeuristic = histogramPartitionHeuristic(),
              clusterIdentifier: ClusterIdentifier[Any] = uuidClusterIdentifier): List[Cluster[Any]] =
    createClusters(
      data,
      coords,
      clusterLabels(data, distanceFunction, method, partitionHeuristic),
      clusterIdentifier)


  /**
    * @param data The LabeledPoint instances to cluster.
    * @param distanceFunction The distance function.
    * @param method The hierarchical clustering method: single, complete, etc. Default = "single".
    * @param partitionHeuristic The hierarchical clustering cutoff heuristic.
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


  private def createClusters(data: Seq[LabeledPoint],
                             coords: HyperCubeCoordinateVector,
                             clusterLabels: Array[Int],
                             clusterIdentifier: ClusterIdentifier[Any]): List[Cluster[Any]] =
    clusterLabels
      .zipWithIndex
      .map{ case (clusterLabel, pointIdx) => (clusterLabel, data(pointIdx)) }
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map{ case (clusterLabel, points) => Cluster(clusterIdentifier(clusterLabel), coords, points.toSet) }
      .toList

}
