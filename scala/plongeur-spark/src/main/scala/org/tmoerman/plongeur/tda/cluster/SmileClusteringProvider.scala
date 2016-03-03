package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import smile.clustering.HierarchicalClustering
import smile.clustering.linkage._

import scala.util.Try

/**
  * Recycled a few methods from smile-scala, which is not released as a Maven artifact.
  *
  * @author Thomas Moerman
  */
object SmileClusteringProvider extends LocalClusteringProvider with Serializable {

  /**
    * @see LocalClusteringProvider
    */
  def apply(dataPoints: List[DataPoint],
            distanceFunction: DistanceFunction = euclidean,
            clusteringMethod: ClusteringMethod = SINGLE) = new LocalClustering {

      lazy val distances = distanceMatrix(dataPoints, distanceFunction)

      lazy val linkage = createLinkage(clusteringMethod, distances)

      lazy val hierarchicalClustering = new HierarchicalClustering(linkage)

      override def heights(includeDiameter: Boolean = true): Seq[Double] =
        if (includeDiameter)
          hierarchicalClustering.getHeight :+ distances.flatten.max
        else
          hierarchicalClustering.getHeight

      override def labels(scaleSelection: ScaleSelection): Seq[Any] =
        dataPoints match {
          case      Nil => Nil
          case _ :: Nil => 0 :: Nil
          case _        =>
            val cutoff = scaleSelection(heights(true))

            lazy val attempt = hierarchicalClustering.partition(cutoff).toSeq
            lazy val backup  = Stream.fill(dataPoints.size)(0)

            Try(attempt).getOrElse(backup)
        }

    }

  def createLinkage(method: ClusteringMethod, distanceMatrix: DistanceMatrix) =
    method.toString.toLowerCase match {
      case "complete" => new CompleteLinkage(distanceMatrix)
      case "single"   => new SingleLinkage(distanceMatrix)
      case "ward"     => new WardLinkage(distanceMatrix)
      case "upgma"    => new UPGMALinkage(distanceMatrix)
      case "upgmc"    => new UPGMCLinkage(distanceMatrix)
      case "wpgma"    => new WPGMALinkage(distanceMatrix)
      case "wpgmc"    => new WPGMCLinkage(distanceMatrix)

      case _ => throw new IllegalArgumentException(s"Unknown linkage method: $method")
    }

}
