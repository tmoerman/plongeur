package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._

import scalaz.Memo.mutableHashMapMemo

/**
  * @author Thomas Moerman
  */
object Clustering extends Serializable {

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

  /**
    * Protocol for constructing Clustering instances.
    */
  trait LocalClusteringProvider {

    def apply(dataPoints: List[DataPoint],
              distanceFunction: DistanceFunction = euclidean,
              clusteringMethod: ClusteringMethod = SINGLE): LocalClustering

  }

  def localClusters[ID](levelSetID: LevelSetID,
                        dataPoints: Seq[DataPoint],
                        clusterLabels: Seq[Any],
                        clusterIDGenerator: ClusterIDGenerator[ID]): List[Cluster[ID]] = {

    val clusterIdentifier: Any => ID = mutableHashMapMemo(_ => clusterIDGenerator())

    clusterLabels
      .zip(dataPoints)
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map { case (clusterLabel, points) =>
        Cluster(
          clusterIdentifier(clusterLabel),
          levelSetID,
          points.toSet)
      }
      .toList
  }

}
