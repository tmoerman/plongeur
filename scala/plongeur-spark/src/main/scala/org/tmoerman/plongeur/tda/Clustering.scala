package org.tmoerman.plongeur.tda

import java.util.UUID
import java.util.UUID._

import org.apache.spark.mllib.regression.LabeledPoint

import smile.clustering.HierarchicalClustering
import smile.clustering.linkage._
import smile.math.distance.Distance
import scalaz.Memo

/**
  * Recycled a few methods from smile-scala, which is not released as a Maven artifact.
  *
  * @author Thomas Moerman
  */
object Clustering extends Serializable {

  // TODO use Breeze distance metrics instead of SMILE Distance

  implicit def lift(dist: Distance[Array[Double]]): Distance[LabeledPoint] = new Distance[LabeledPoint] {

    override def d(l1: LabeledPoint, l2: LabeledPoint) = dist.d(l1.features.toArray, l2.features.toArray)

  }

  //type DistanceFunction[T] = (T, T) => Double

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
      // some optimization going on here

      val (small, large) =
        if (this.points.size < that.points.size)
          (this.labels, that.labels) else
          (that.labels, this.labels)

      small.exists(e => large.contains(e))
    }

  }

  def uuidCluster(points: Set[LabeledPoint]) = new Cluster(randomUUID, points)

  type ClusterIdentifier[ID] = (Int) => ID

  def uuidClusterIdentifier: ClusterIdentifier[UUID] = Memo.mutableHashMapMemo(_ => {println("tetten"); randomUUID})

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

  def distanceMatrix[T](data: List[T], dist: Distance[T]): DistanceMatrix = {
    val n = data.length
    val result = new Array[Array[Double]](n)

    for (i <- 0 until n) {
      result(i) = new Array[Double](i+1)
      for (j <- 0 until i)
        result(i)(j) = dist.d(data(i), data(j))
    }

    result
  }

  type PartitionHeuristic = (HierarchicalClustering) => Int // TODO height cutoff perhaps?

  /**
    *
    * @param data The LabeledPoint instances to cluster.
    * @param dist The distance function.
    * @param identifier A function that translates the cluster label to a global cluster ID.
    * @param method The hierarchical clustering method: single, complete, etc. Default = "single".
    * @param heur The hierarchical clustering cutoff heuristic
    * @tparam ID The generic cluster ID type.
    * @return Returns a list of Cluster instances.
    */
  def cluster[ID](data: List[LabeledPoint],
                  dist: Distance[LabeledPoint],
                  identifier: ClusterIdentifier[ID],
                  method: String = "single",
                  heur: PartitionHeuristic = (_) => 4): List[Cluster[ID]] = {

    val distances = distanceMatrix(data, dist)

    val hierarchicalClustering = new HierarchicalClustering(linkage(method, distances))

    val k = heur(hierarchicalClustering)

    hierarchicalClustering
      .partition(k)
      .zipWithIndex
      .map{ case (clusterLabel, pointIdx) => (clusterLabel, data(pointIdx)) }
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map{ case (clusterLabel, points) => Cluster(identifier(clusterLabel), points.toSet) }
      .toList
  }

}
