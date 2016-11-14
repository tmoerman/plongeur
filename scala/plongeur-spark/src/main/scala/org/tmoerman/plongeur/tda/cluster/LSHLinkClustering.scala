package org.tmoerman.plongeur.tda.cluster

import org.tmoerman.plongeur.tda.Distances.{DEFAULT_DISTANCE, DistanceFunction}
import org.tmoerman.plongeur.tda.Model.DataPoint

/**
  * @author Thomas Moerman
  */
object LSHLinkClustering {

  case class LSHLinkClusteringParams(val k: Int,
                                     val L: Int,
                                     val A: Double,
                                     val distance: DistanceFunction = DEFAULT_DISTANCE) extends Serializable {

  }

  def run(points: Iterable[DataPoint], params: LSHLinkClusteringParams) = {

    def maxCoordinate(points: Iterable[DataPoint]) = points.map(p => p.features(p.features.argmax)).max

    val r = maxCoordinate(points)
    val n = points.size

    // @tailrec
    def step(phase: Int = 1, nrClusters: Int = n) = {


      ???
    }

  }

}