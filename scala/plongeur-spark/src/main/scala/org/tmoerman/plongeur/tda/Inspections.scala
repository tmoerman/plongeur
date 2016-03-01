package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.Clustering.Cluster
import org.tmoerman.plongeur.tda.Skeleton.TDAResult
import org.tmoerman.plongeur.util.IterableFunctions

import scalaz.Memo

/**
  * Inspections on a TDA result only needed in debugging/analysis context.
  *
  * @author Thomas Moerman
  */
object Inspections {

  implicit def pimpTdaResult(result: TDAResult)(implicit counter: (Any => Int)): TDAResultInspections =
    new TDAResultInspections(result, counter)

  def mapToInt: (Any => Int) = {
    val source = Stream.iterate(0)(_ + 1).iterator
    Memo.mutableHashMapMemo(_ => source.next)
  }

}

class TDAResultInspections(val result: TDAResult,
                           val clusterCounter: (Any => Int)) extends Serializable {

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
      .map(cluster => "[c" + clusterCounter(cluster.id) + "]" + " -> " + cluster.points.map(_.index).toList.sorted.mkString(", "))

  type CA = Cluster[Any]

  import IterableFunctions._

  def levelSetsToClusters =
    result
        .clustersRDD
        .map(cluster => (cluster.levelSetID, cluster))
        .combineByKey(
          (cluster: CA) => Set(cluster),
          (acc: Set[CA], c: CA) => acc + c,
          (acc1: Set[CA], acc2: Set[CA]) => acc1 ++ acc2)
        .sortBy(_._1)
        .collect
        .map{ case (levelSetID, clusters) => levelSetID.mkString(" ") + " [" + clusters.flatMap(_.points).map(_.index).min + ", " + clusters.flatMap(_.points).map(_.index).max + "]" + "\n" +
          clusters.map(cluster => "  [c" + clusterCounter(cluster.id) + "]" + " -> " + cluster.points.map(_.index).toList.sorted.mkString(", ")).mkString("\n")
        }

  def pointsToClusters =
    result
      .clustersRDD
      .flatMap(cluster => cluster.points.map(p => (p.index, cluster.id)))
      .combineByKey(
        (clusterId: Any) => Set(clusterId),
        (acc: Set[Any], id: Any) => acc + id,
        (acc1: Set[Any], acc2: Set[Any]) => acc1 ++ acc2)
      .sortBy(_._1)
      .collect
      .map{ case (point, clusterIds) => point + " -> " + clusterIds.map(clusterCounter).toList.sorted.mkString(", ") }

}

