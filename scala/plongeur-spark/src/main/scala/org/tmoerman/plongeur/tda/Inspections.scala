package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.Clustering.Cluster
import org.tmoerman.plongeur.tda.Skeleton.TDAResult

import scala.collection.mutable

/**
  * @author Thomas Moerman
  */
object Inspections {

  implicit def pimpTdaResult(result: TDAResult)(implicit counters: ((Any => Int), (Any => Int))): TDAResultInspections =
    new TDAResultInspections(result, counters)

}

class MapToInt extends (Any => Int) {

  private[this] var i = 0

  def next: Int = {
    val result = i

    i+=1

    result
  }

  private[this] val cache = mutable.Map[Any, Int]()

  def apply(a: Any): Int = cache.getOrElseUpdate(a, next)

}

class TDAResultInspections(val result: TDAResult,
                           val counters: ((Any => Int), (Any => Int))) extends Serializable {

  import org.tmoerman.plongeur.util.IterableFunctions._

  val clusterCounter = counters._1
  val coordsCounter = counters._2

  def dotGraph(name: String) =
    Seq(
      s"graph $name {",
      result.clusters.map(_.id).map(clusterCounter).mkString("\n"),
      result.edges
        .map(_.map(clusterCounter))
        .map(_.toArray match { case Array(x, y) => s"$x -- $y" })
        .mkString("\n"),
      "}").mkString("\n")

  def clusterPoints =
    result
      .clusters
      .sortBy(_.id.toString)
      .map(cluster => "[c" + clusterCounter(cluster.id) + "]" + " -> " + cluster.points.map(_.label.toInt).toList.sorted.mkString(", "))

  type CA = Cluster[Any]

  def coordsToClusters =
    result
        .clustersRDD
        .map(cluster => (cluster.coords, cluster))
        .combineByKey(
          (cluster: CA) => Set(cluster),
          (acc: Set[CA], c: CA) => acc + c,
          (acc1: Set[CA], acc2: Set[CA]) => acc1 ++ acc2)
        .sortBy(_._1)
        .collect
        .map{ case (coords, clusters) => coords.mkString(" ") + " [" + clusters.flatMap(_.points).map(_.label.toInt).min + ", " + clusters.flatMap(_.points).map(_.label.toInt).max + "]" + "\n" +
          clusters.map(cluster => "  [c" + clusterCounter(cluster.id) + "]" + " -> " + cluster.points.map(_.label.toInt).toList.sorted.mkString(", ")).mkString("\n")
        }

  def pointsToClusters =
    result
      .clustersRDD
      .flatMap(cluster => cluster.points.map(p => (p.label.toInt, cluster.id)))
      .combineByKey(
        (clusterId: Any) => Set(clusterId),
        (acc: Set[Any], id: Any) => acc + id,
        (acc1: Set[Any], acc2: Set[Any]) => acc1 ++ acc2)
      .sortBy(_._1)
      .collect
      .map{ case (point, clusterIds) => point + " -> " + clusterIds.map(clusterCounter).toList.sorted.mkString(", ") }

}

