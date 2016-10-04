package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClustering.createLocalClustering
import smile.clustering.HierarchicalClustering
import smile.clustering.linkage._

import scala.util.Try

object SmileClustering extends Serializable {

  def createLocalClustering(localDataPoints: Seq[DataPoint],
                            distances: Array[Array[Double]],
                            clusteringMethod: String) = new LocalClustering {

    val linkage = createLinkage(clusteringMethod, distances)

    val hierarchicalClustering = new HierarchicalClustering(linkage)

    override def heights(includeDiameter: Boolean = true): Seq[Double] =
      if (includeDiameter)
        hierarchicalClustering.getHeight :+ distances.flatten.max
      else
        hierarchicalClustering.getHeight

    override def labels(scaleSelection: ScaleSelection): Seq[Any] =
      localDataPoints match {
        case Nil => Nil
        case _ :: Nil => 0 :: Nil
        case _ =>
          val cutoff = scaleSelection(heights(true))

          lazy val attempt = hierarchicalClustering.partition(cutoff).toSeq
          lazy val backup = Stream.fill(localDataPoints.size)(0)

          Try(attempt).getOrElse(backup)
      }

    override def debug = hierarchicalClustering.getTree.map(_.mkString(", ")).mkString("\n")

  }

  private def createLinkage(method: String, distanceMatrix: Array[Array[Double]]) =
    method.toLowerCase match {
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

/**
  * Recycled a few methods from smile-scala, which is not released as a Maven artifact.
  *
  * @author Thomas Moerman
  */
object SimpleSmileClusteringProvider extends LocalClusteringProvider with Serializable {

  /**
    * @see LocalClusteringProvider
    */
  def apply(localDataPoints: Seq[DataPoint], params: ClusteringParams = ClusteringParams()): LocalClustering = {
    import params._

    val distanceFunction = parseDistance(distanceSpec)

    val distances = distanceMatrix(localDataPoints, distanceFunction)

    createLocalClustering(localDataPoints, distances, clusteringMethod)
  }

}