package org.tmoerman.plongeur.tda

import java.util.UUID
import java.util.UUID._

import org.apache.spark.mllib.regression.LabeledPoint
import org.tmoerman.plongeur.tda.Distance.{DistanceFunction, euclidean}
import org.tmoerman.plongeur.util.IterableFunctions._

import smile.clustering.HierarchicalClustering
import smile.clustering.linkage._
import scalaz.Memo.mutableHashMapMemo

/**
  * Recycled a few methods from smile-scala, which is not released as a Maven artifact.
  *
  * @author Thomas Moerman
  */
object Clustering extends Serializable {

  type DistanceMatrix = Array[Array[Double]]

  type ClusterID = UUID

  case class Cluster[ID](val id: ID,
                         val points: Set[LabeledPoint]) extends Serializable {

    lazy val labels = points.map(_.label)

    /**
      * @param that a Cluster.
      * @return Returns whether any point is common to both clusters.
      */
    def intersects(that: Cluster[ID]) = {
      val (small, large) = // some optimization going on here
        if (this.points.size < that.points.size)
          (this.labels, that.labels) else
          (that.labels, this.labels)

      small.exists(e => large.contains(e))
    }

    override def toString = s"""Cluster($id, ${labels.mkString(", ")})"""

  }

  def uuidCluster(points: Set[LabeledPoint]) = new Cluster(randomUUID, points)

  type ClusterIdentifier[ID] = (Int) => ID

  def uuidClusterIdentifier: ClusterIdentifier[UUID] = mutableHashMapMemo(_ => randomUUID)

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

  def histogramPartitionHeuristic(k: Int = 10) = (heights: Heights) => {

    val inc = (heights.max - heights.min) / k

    val frequencies = heights.map(d => (BigDecimal(d) quot inc).toInt).frequencies

    (frequencies.keys.min to frequencies.keys.max)
      .dropWhile(x => frequencies(x) != 0)
      .headOption
      .getOrElse(1) * inc
  }

  /**
    * @param dist The distance function.
    * @param method The hierarchical clustering method: single, complete, etc. Default = "single".
    * @param partitionHeuristic The hierarchical clustering cutoff heuristic
    *
    * @param data The LabeledPoint instances to cluster.
    *
    * @return Returns a list of Cluster instances.
    */
  def cluster(dist: DistanceFunction = euclidean,
              method: String = "single",
              partitionHeuristic: PartitionHeuristic = histogramPartitionHeuristic())
             (data: Seq[LabeledPoint]): List[Cluster[Int]] = {

    val distanceMatrix = distances(data, dist)

    val hierarchicalClustering = new HierarchicalClustering(linkage(method, distanceMatrix))

    val cutoffHeight = partitionHeuristic(hierarchicalClustering.getHeight)

    hierarchicalClustering
      .partition(cutoffHeight)
      .zipWithIndex
      .map{ case (clusterLabel, pointIdx) => (clusterLabel, data(pointIdx)) }
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map{ case (clusterLabel, points) => Cluster(clusterLabel, points.toSet) }
      .toList
  }

}
