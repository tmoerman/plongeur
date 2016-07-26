package org.tmoerman.plongeur.tda

import breeze.linalg.functions._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.VectorConversions._
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model._
import shapeless._
import org.tmoerman.plongeur.util.RDDFunctions._

/**
  * @author Thomas Moerman
  */
object Distance {

  /**
    * See smile-scala.
    *
    * @param dataPoints       The data points.
    * @param distanceFunction The distance function, e.g. Euclidean.
    * @return Returns a distance matrix, represented as an array of arrays of doubles.
    */
  def distanceMatrix(dataPoints: Seq[DataPoint],
                     distanceFunction: DistanceFunction): Array[Array[Double]] = {
    val n = dataPoints.length
    val result = new Array[Array[Double]](n)

    for (i <- 0 until n) {
      result(i) = new Array[Double](i + 1)
      for (j <- 0 until i) {
        result(i)(j) = distanceFunction(dataPoints(i), dataPoints(j))
      }
    }

    result
  }

  /**
    * @param dataPoints An RDD of data points.
    * @param distanceFunction The distance function, e.g. Euclidean.
    * @return Returns a distance matrix, represented by an RDD of pairs keyed by
    *         the set of DataPoint indices and value the distance ith respect to the distance function.
    */
  def distanceMatrix(dataPoints: RDD[DataPoint],
                     distanceFunction: DistanceFunction): RDD[(Set[Index], Double)] =
    dataPoints
      .distinctComboPairs
      .map{ case ((p1, p2)) => (Set(p1.index, p2.index), distanceFunction(p1, p2)) }

  def toBroadcastAmendment(spec: HList, ctx: TDAContext): Option[(String, () => Broadcast[Any])] =
    toBroadcastKey(spec).map(key => (key, () => {
      val v: Any = toBroadcastDistanceMatrix(spec, ctx)

      ctx.sc.broadcast(v)
    }))

  def toBroadcastDistanceMatrix(spec: HList, ctx: TDAContext) =
    distanceMatrix(ctx.dataPoints, parseDistance(spec)).collectAsMap

  def toBroadcastKey(spec: HList): Option[String] = spec match {
    case name :: HNil             => Some(s"$name")
    case name :: (e: Any) :: HNil => Some(s"$name[$e]")
    case _                        => None
  }

  trait DistanceFunction extends ((DataPoint, DataPoint) => Double) with Serializable {
    override def toString = getClass.getSimpleName
  }

  // TODO Pearson correlation, closing over ~~TDAContext~~ / over broadcast variable

  // TODO Spearman

  case object ChebyshevDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = chebyshevDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case object CosineDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = cosineDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case object EuclideanDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = euclideanDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case object ManhattanDistance extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = manhattanDistance(a.features.toBreeze, b.features.toBreeze)
  }

  case class MinkowskiDistance(exponent: Double) extends DistanceFunction {
    override def apply(a: DataPoint, b: DataPoint) = minkowskiDistance(a.features.toBreeze, b.features.toBreeze, exponent)
    override def toString = { val name = getClass.getSimpleName; s"$name($exponent)" }
  }

  /**
    * @param spec
    * @return Returns a DistanceFunction for specified spec.
    */
  def parseDistance(spec: HList): DistanceFunction = spec match {
    case "chebyshev" :: HNil             => ChebyshevDistance
    case "cosine"    :: HNil             => CosineDistance
    case "euclidean" :: HNil             => EuclideanDistance
    case "manhattan" :: HNil             => ManhattanDistance
    case "minkowski" :: (e: Any) :: HNil => MinkowskiDistance(e.asInstanceOf[Double])

    case _                               => EuclideanDistance
  }

}
