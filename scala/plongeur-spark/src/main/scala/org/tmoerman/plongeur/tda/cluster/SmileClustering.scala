package org.tmoerman.plongeur.tda.cluster

import org.apache.spark.Logging
import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.SmileClustering.createLocalClustering
import smile.clustering.HierarchicalClustering
import smile.clustering.linkage._

import scala.util.Try

object SmileClustering extends Serializable {

  def createLocalClustering(localDataPoints: Seq[DataPoint],
                            distances: DistanceMatrix,
                            clusteringMethod: String) = new LocalClustering {

    lazy val linkage = createLinkage(clusteringMethod, distances)

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
  }

  private def createLinkage(method: String, distanceMatrix: DistanceMatrix) =
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
  def apply(localDataPoints: Seq[DataPoint],
            params: ClusteringParams = ClusteringParams()) = {

    import params._

    lazy val distanceFunction = parseDistance(distanceSpec)

    lazy val distances = distanceMatrix(localDataPoints, distanceFunction)

    createLocalClustering(localDataPoints, distances, clusteringMethod)
  }

}

import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * Broadcasts the entire distance matrix for a specified distance function.
  *
  * @param ctx
  */
class BroadcastSmileClusteringProvider(val ctx: TDAContext)
  extends LocalClusteringProvider with Serializable with Logging {

  def computeDistanceMatrixForBroadcast(distanceFunction: DistanceFunction) = {
    logWarning(s"computing distance matrix $distanceFunction")

    ctx.dataPoints.distanceMatrix(distanceFunction).collectAsMap
  }

  /**
    * @see LocalClusteringProvider
    */
  def apply(localDataPoints: Seq[DataPoint],
            params: ClusteringParams = ClusteringParams()) = {

    import ctx._
    import params._

    val distanceFunction = parseDistance(distanceSpec)

    lazy val bc = sc.broadcast(computeDistanceMatrixForBroadcast(distanceFunction))

    lazy val bcDistanceFunction = new DistanceFunction {
      def apply(p1: DataPoint, p2: DataPoint) = bc.value.apply(Set(p1.index, p2.index))

      override def toString = s"broadcast $distanceFunction"
    }

    lazy val distances = distanceMatrix(localDataPoints, bcDistanceFunction)

    createLocalClustering(localDataPoints, distances, clusteringMethod)
  }

}