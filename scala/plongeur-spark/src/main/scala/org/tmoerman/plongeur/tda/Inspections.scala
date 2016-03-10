package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.TDA._
import org.tmoerman.plongeur.util.IterableFunctions

import scalaz.Memo

/**
  * Inspections on a TDA result only needed in debugging/analysis context.
  *
  * @author Thomas Moerman
  */
object Inspections {

  implicit def pimpTdaResult(result: TDAResult)(implicit counter: (Any => Int)): TDAResultInspections = new TDAResultInspections(result, counter)

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
      result.clusters.map(_.id).map(clusterCounter).sorted.mkString("\n"),
      result.edges
        .map(_.map(clusterCounter))
        .map(_.toArray match { case Array(x, y) => s"$x -- $y" })
        .sorted
        .mkString("\n"),
      "}").mkString("\n")


  def connectedComponents: Seq[Set[Int]] = Nil // following is wrong -> use edges instead!

//    result
//      .clusters
//      .foldLeft(Set[Set[Cluster[ID]]]()) { case (acc, cluster) =>
//        acc
//          .find(component => component.exists(_.dataPoints.toSet.intersect(cluster.dataPoints.toSet).nonEmpty))
//          .map(found => (acc - found) + (found + cluster))
//          .getOrElse(acc + Set(cluster))
//      }
//      .toSeq
//      .map(component => component.map(cluster => clusterCounter(cluster.id)))


  def clusterPoints =
    result
      .clusters
      .sortBy(_.id.toString)
      .map(cluster => "[c." + clusterCounter(cluster.id) + "]" + " -> " + cluster.dataPoints.map(_.index).toList.sorted.mkString(", "))

  import IterableFunctions._

  def levelSetsToClusters =
    result
        .clustersRDD
        .keyBy(_.levelSetID)
        .groupByKey
        .sortBy(_._1)
        .collect
        .map{ case (levelSetID, clusters) => levelSetID.mkString(" ") + " [" + clusters.flatMap(_.dataPoints).map(_.index).min + ", " + clusters.flatMap(_.dataPoints).map(_.index).max + "]" + "\n" +
          clusters
            .toSeq
            .map(cluster => "  [c." + clusterCounter(cluster.id) + "]" + " -> " + cluster.dataPoints.map(_.index).toList.sorted.mkString(", "))
            .sorted
            .mkString("\n")
        }

  def pointsToClusters =
    result
      .clustersRDD
      .flatMap(cluster => cluster.dataPoints.map(p => (p.index, cluster.id)))
      .groupByKey
      .sortBy(_._1)
      .collect
      .map{ case (point, clusterIds) => point + " -> " + clusterIds.map(clusterCounter).toList.sorted.mkString(", ") }

}

