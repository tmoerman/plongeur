package org.tmoerman.plongeur.tda

import org.apache.commons.lang.StringUtils._
import org.tmoerman.plongeur.tda.Clustering.Cluster
import org.tmoerman.plongeur.tda.Skeleton.TDAResult

/**
  * @author Thomas Moerman
  */
object Inspections {

  implicit def pimpTdaResult(result: TDAResult): TDAResultInspections = new TDAResultInspections(result)

}

class TDAResultInspections(val result: TDAResult) extends Serializable {

  def clean(s: Any) = replaceChars(s.toString, "-", "_")

  def dotGraph(name: String) =
    Seq(
      s"graph $name {",
      result.clusters.map(_.id).map(clean).mkString("\n"),
      result.edges
        .map(_.map(clean))
        .map(_.toArray match { case Array(x, y) => s"$x -- $y" })
        .mkString("\n"),
      "}").mkString("\n")

  def clusterPoints =
    result
      .clusters
      .map{case Cluster(id, points) => points.map(_.label).toList.sorted.mkString(", ")}
      .sorted


}

