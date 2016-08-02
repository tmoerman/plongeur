package org.tmoerman.plongeur.tda.cluster

import java.util.UUID
import java.util.UUID._

import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Scale._
import shapeless.{HNil, HList}

import scalaz.Memo.mutableHashMapMemo

/**
  * @author Thomas Moerman
  */
object Clustering extends Serializable {

  /**
    * Function that takes a Seq of clustering heights as input and returns a clustering height as output.
    */
  trait ScaleSelection extends (Seq[Double] => Double) {

    def resolution: Int

  }

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

  /**
    * @param distanceSpec Distance function in the hierarchical clustering effort.
    * @param clusteringMethod Single, Complete, etc...
    * @param partitionByLevelSetID Activate RangePartitioning by levelSet ID, is probably more efficient than hash
    *                              partitioning on LevelSetIDs.
    */
  case class ClusteringParams(distanceSpec: HList  = "euclidean" :: HNil,
                              clusteringMethod: String = "single",
                              partitionByLevelSetID: Boolean = true) extends Serializable

  /**
    * Protocol for constructing Clustering instances.
    */
  trait LocalClusteringProvider {

    def apply(dataPoints: Seq[DataPoint], params: ClusteringParams): LocalClustering

  }

  /**
    * @param levelSetID ID of the level set to which this cluster belongs.
    * @param dataPoints The data points to assign to clusters.
    * @param clusterLabels The cluster labels, in the same order as the data points.
    * @return Returns a List of Cluster instances with global IDs generated in function of the specified local
    *         cluster labels.
    */
  def localClusters(levelSetID: LevelSetID,
                    dataPoints: Seq[DataPoint],
                    clusterLabels: Seq[Any]): List[Cluster] = {

    val clusterID: Any => UUID = mutableHashMapMemo(_ => randomUUID)

    clusterLabels
      .zip(dataPoints)
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .map{ case (clusterLabel, points) => Cluster(clusterID(clusterLabel), levelSetID, points.toSet) }
      .toList
  }

}
