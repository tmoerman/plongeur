package org.tmoerman.plongeur.tda

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

  def json(result: TDAResult) = {
    import play.api.libs.json._

    val r = scala.util.Random

    JsObject(
      Seq(
        "nodes" -> JsArray(
          result.clusters.map(c => {
            JsObject(Seq(
              "id"    -> JsString(c.id.toString),
              "label" -> JsString(c.id.toString),
              "size"  -> JsNumber(c.dataPoints.size),
              "x"     -> JsNumber(r.nextInt(100)),
              "y"     -> JsNumber(r.nextInt(100))))
          })),
        "edges" -> JsArray(
          result.edges.map(e => {
            val (from, to) = e.toArray match {case Array(f, t) => (f, t)}
            JsObject(Seq(
              "id"     -> JsString(s"$from--$to"),
              "source" -> JsString(from.toString),
              "target" -> JsString(to.toString)))
          }))))
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


  def connectedComponents: Seq[Set[Int]] = Nil // TODO complete

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