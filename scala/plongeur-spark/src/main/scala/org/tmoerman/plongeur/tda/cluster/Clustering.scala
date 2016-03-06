package org.tmoerman.plongeur.tda.cluster

import java.util.UUID
import java.util.UUID._

import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Scale._

import scalaz.Memo.mutableHashMapMemo

/**
  * @author Thomas Moerman
  */
object Clustering extends Serializable {

  /**
    * Function that takes a Seq of clustering heights as input and returns a clustering height as output.
    */
  type ScaleSelection = (Seq[Double]) => Double

  /**
    * Project specific protocol for hierarchical clustering. Not to be confused with the SMILE implementation.
    */
  trait LocalClustering extends Serializable {

    /**
      * @param includeDiameter Flag that specifies whether the diameter (= max distance in the DistanceMatrix)
      *                        should be included in the heights of the hierarchical clustering.
      * @return Returns the heights.
      */
    def heights(includeDiameter: Boolean = true): Seq[Double]

    /**
      * @param scaleSelection The scale selection method.
      * @return Returns the cluster labels in function of the
      */
    def labels(scaleSelection: ScaleSelection): Seq[Any]
    
  }

  sealed trait ClusteringMethod
  case object COMPLETE extends ClusteringMethod
  case object SINGLE   extends ClusteringMethod
  case object WARD     extends ClusteringMethod

  type ClusterIDGenerator[ID] = () => ID

  def uuidClusterIDGenerator: ClusterIDGenerator[UUID] = () => randomUUID

  /**
    * @param distanceFunction Distance function in the hierarchical clustering effort.
    * @param clusteringMethod Single, Complete, etc...
    * @param clusterIDGenerator The cluster ID generator function.
    * @param collapseDuplicateClusters Eliminates identical clusters.
    * @param scaleSelection The clustering scale selection function.
    * @tparam ID Cluster ID type parameter
    */
  case class ClusteringParams[ID](distanceFunction: DistanceFunction = euclidean,
                                  clusteringMethod: ClusteringMethod = SINGLE,
                                  clusterIDGenerator: ClusterIDGenerator[ID],
                                  collapseDuplicateClusters: Boolean = true,
                                  scaleSelection: ScaleSelection = histogram()) extends Serializable

  /**
    * Protocol for constructing Clustering instances.
    */
  trait LocalClusteringProvider {

    def apply(dataPoints: List[DataPoint],
              distanceFunction: DistanceFunction = euclidean,
              clusteringMethod: ClusteringMethod = SINGLE): LocalClustering

  }

  /**
    * @param levelSetID ID of the level set to which this cluster belongs.
    * @param dataPoints The data points to assign to clusters.
    * @param clusterLabels The cluster labels, in the same order as the data points.
    * @param clusterIDGenerator Cluster ID generator function, usually a UUID generator
    * @tparam ID Cluster ID type parameter
    * @return Returns a List of Cluster instances with global IDs generated in function of the specified local
    *         cluster labels.
    */
  def localClusters[ID](levelSetID: LevelSetID,
                        dataPoints: Seq[DataPoint],
                        clusterLabels: Seq[Any],
                        clusterIDGenerator: ClusterIDGenerator[ID]): List[Cluster[ID]] = {

    val clusterID: Any => ID = mutableHashMapMemo(_ => clusterIDGenerator())

    clusterLabels
      .zip(dataPoints)
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map{ case (clusterLabel, points) => Cluster(clusterID(clusterLabel), levelSetID, points.toSet) }
      .toList
  }

}
