package org.tmoerman.plongeur.tda

import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.TDA._
import org.tmoerman.plongeur.util.IterableFunctions

import scala.reflect.ClassTag
import scalaz.Memo

/**
  * Inspections on a TDA result only needed in debugging/analysis context.
  *
  * @author Thomas Moerman
  */
object Inspections {

  implicit def pimpTdaResult[ID](result: TDAResult[ID])(implicit counter: (Any => Int), tag: ClassTag[ID]): TDAResultInspections[ID] = new TDAResultInspections[ID](result, counter)(tag)

  def mapToInt: (Any => Int) = {
    val source = Stream.iterate(0)(_ + 1).iterator
    Memo.mutableHashMapMemo(_ => source.next)
  }

}

class TDAResultInspections[ID](val result: TDAResult[ID],
                               val clusterCounter: (Any => Int))
                              (implicit tag: ClassTag[ID]) extends Serializable {

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
      .map(cluster => "[c" + clusterCounter(cluster.id) + "]" + " -> " + cluster.dataPoints.map(_.index).toList.sorted.mkString(", "))

  type C = Cluster[ID]

  import IterableFunctions._

  def levelSetsToClusters =
    result
        .clustersRDD
        .keyBy(_.levelSetID)
        .combineByKey(
          (cluster: C) => Set(cluster),
          (acc: Set[C], c: C) => acc + c,
          (acc1: Set[C], acc2: Set[C]) => acc1 ++ acc2)
        .sortBy(_._1)
        .collect
        .map{ case (levelSetID, clusters) => levelSetID.mkString(" ") + " [" + clusters.flatMap(_.dataPoints).map(_.index).min + ", " + clusters.flatMap(_.dataPoints).map(_.index).max + "]" + "\n" +
          clusters.map(cluster => "  [c" + clusterCounter(cluster.id) + "]" + " -> " + cluster.dataPoints.map(_.index).toList.sorted.mkString(", ")).mkString("\n")
        }

  def pointsToClusters =
    result
      .clustersRDD
      .flatMap(cluster => cluster.dataPoints.map(p => (p.index, cluster.id)))
      .combineByKey(
        (clusterId: Any) => Set(clusterId),
        (acc: Set[Any], id: Any) => acc + id,
        (acc1: Set[Any], acc2: Set[Any]) => acc1 ++ acc2)
      .sortBy(_._1)
      .collect
      .map{ case (point, clusterIds) => point + " -> " + clusterIds.map(clusterCounter).toList.sorted.mkString(", ") }

}

