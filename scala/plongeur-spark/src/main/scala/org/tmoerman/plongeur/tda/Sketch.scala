package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.lang.Math.{min, pow, sqrt}

import breeze.linalg.DenseVector.fill
import com.github.karlhigley.spark.neighbors.lsh.{LSHFunction, ScalarRandomProjectionFunction, SignRandomProjectionFunction, Signature}
import org.apache.spark.mllib.linalg.VectorConversions._
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model.{DataPoint, Index, SimpleName, TDAContext}
import org.tmoerman.plongeur.tda.Sketch.SketchParams

import scala.annotation.tailrec
import scala.util.Random
import scala.util.hashing.MurmurHash3

import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * @author Thomas Moerman
  */
object Sketch extends Serializable {

  /**
    * @param k The signature length.
    * @param r The radius.
    * @param distance The distance function, necessary for choosing the hashing strategy.
    * @param prototypeStrategy The strategy for collapsing colliding points into a prototype point.
    * @param random A JVM random.
    */
  case class SketchParams(k: Int,
                          r: Double,
                          distance: DistanceFunction,
                          prototypeStrategy: PrototypeStrategy,
                          random: Random = new Random()) {

    override def toString = s"Sketch(dist=$distance, "

  }

  def estimateSketchParams(ctx: TDAContext): SketchParams = {
    ???
  }

  case class Prototype(val features: MLVector,
                       final val index: Int = -1,
                       final val meta: Option[Map[String, _ <: Serializable]] = None) extends DataPoint with Serializable

  trait PrototypeStrategy extends (RDD[(Any, DataPoint)] => RDD[(List[Index], DataPoint)]) with SimpleName with Serializable

  case class RandomCandidate(random: Random = new Random()) extends PrototypeStrategy {

    type ACC = (List[Index], DataPoint)

    def init(d: DataPoint): ACC = (Nil, d)

    def concat(acc: ACC, d: DataPoint): ACC = acc match {
      case (indices, proto) => (d.index :: indices, Prototype((if (random.nextBoolean) proto else d).features))
    }

    def merge(acc1: ACC, acc2: ACC): ACC = (acc1, acc2) match {
      case ((indices1, proto1), (indices2, proto2)) => (indices1 ::: indices2, Prototype((if (random.nextBoolean) proto1 else proto2).features))
    }

    override def apply(pointsByHashKey: RDD[(Any, DataPoint)]) =
      pointsByHashKey
        .combineByKey(init, concat, merge)
        .values

  }

  case object ArithmeticMean extends PrototypeStrategy {

    type Count = Int

    type ACC = (List[Index], Count, DataPoint)

    def init(d: DataPoint): ACC = (Nil, 1, d)

    def sum(d1: DataPoint, d2: DataPoint) = Prototype((d1.features.toBreeze + d2.features.toBreeze).toMLLib)

    def div(d: DataPoint, scalar: Int) = Prototype((d.features.toBreeze / fill(d.features.size, scalar.toDouble)).toMLLib)

    def concat(acc: ACC, d: DataPoint): ACC = acc match {
      case (indices, count, proto) =>
        (d.index :: indices, count + 1, sum(proto, d))
    }

    def merge(acc1: ACC, acc2: ACC): ACC = (acc1, acc2) match {
      case ((indices1, count1, proto1), (indices2, count2, proto2)) => (indices1 ::: indices2, count1 + count2, sum(proto1, proto2))
    }

    override def apply(pointsByHashKey: RDD[(Any, DataPoint)]) =
      pointsByHashKey
        .combineByKey(init, concat, merge)
        .values
        .map{ case (indices, count, proto) => (indices, div(proto, count)) }

  }

  /**
    * See: "AN EFFICIENT APPROXIMATE ALGORITHM FOR THE 1-MEDIAN PROBLEM IN METRIC SPACES"
    *   -- D. CANTONE†, G. CINCOTTI†, A. FERRO†, AND A. PULVIRENTI†
    *
    * @param t The size of groups on which exactWinner is applied.
    * @param distance The distance function to determine the exact winner.
    * @param random A random generator.
    */
  case class ApproximateMedian(t: Int, distance: DistanceFunction, random: Random = new Random()) extends PrototypeStrategy {

    def exactWinner(S: Iterable[DataPoint]): DataPoint =
      S
        .cartesian
        .map{ case (a, b) => (distance(a, b), a) }
        .minBy(_._1)
        ._2

    def mergeTail(l: List[List[DataPoint]]) = l.reverse match {
      case list @ (x :: y :: rest) => if (x.size < t) (x ++ y) :: rest else list
      case list => list
    }

    def approximateMedian(S: List[DataPoint]): DataPoint = {
      val threshold = min(pow(t, 2), sqrt(S.size)).toInt

      @tailrec def recur(currS: List[DataPoint]): DataPoint =
        if (currS.size < threshold)
          exactWinner(currS)
        else {
          val grouped = random.shuffle(currS).grouped(t).toList

          recur(mergeTail(grouped).map(exactWinner))
        }

      recur(S)
    }

    override def apply(pointsByHashKey: RDD[(Any, DataPoint)]) =
      pointsByHashKey
        .groupByKey
        .map{ case (key, candidatePoints) =>
          val indices = candidatePoints.map(_.index).toList

          val prototype = approximateMedian(candidatePoints.toList)

          (indices, prototype)
        }

  }

  /**
    * @param d The original dimensionality of the data points.
    * @param params
    * @return Returns a random projection LSH function.
    */
  def makeHashFunction(d: Int, params: SketchParams) = {
    import params._

    distance match {
      case CosineDistance      => SignRandomProjectionFunction.generate(d, k, random)
      case EuclideanDistance   => ScalarRandomProjectionFunction.generateL2(d, k, r, random)
      case ManhattanDistance   => ScalarRandomProjectionFunction.generateL1(d, k, r, random)
      case LpNormDistance(0.5) => ScalarRandomProjectionFunction.generateFractional(d, k, r, random)

      case _ => throw new IllegalArgumentException(s"No sketch available for distance function: $distance")
    }
  }

  def apply(ctx: TDAContext, sketchParams: Option[SketchParams] = None): Sketch = {
    val params = sketchParams.getOrElse(estimateSketchParams(ctx))

    val hashFunction = makeHashFunction(ctx.D, params)

    def computeCollisionKey(point: DataPoint): Any = {
      val sig = hashFunction.signature(point.features)

      MurmurHash3.arrayHash(sig.asInstanceOf[Array[Int]])
    }

    val pointsByHashKey: RDD[(Any, DataPoint)] =
      ctx
        .dataPoints
        .map(point => (computeCollisionKey(point), point))

    val prototypes =
      params
        .prototypeStrategy
        .apply(pointsByHashKey)
        .cache

    val N = prototypes.count.toInt

    val prototypesByOrigin = prototypes.flatMap{ case (ids, dp) => ids.map(id => (id, dp)) }.cache

    Sketch(params, hashFunction, N, prototypesByOrigin)
  }

}

/**
  * @param params The SketchParams that were used to create this Sketch.
  * @param hashFunction The randomly selected LSH hash function.
  * @param N The number of prototypes. Note: this is not equal to the size of the RDD.
  * @param prototypesByOrigin An RDD keyed by the indices of the original DataPoint instances to (non-unique)
  *                           collapsed prototypes.
  */
case class Sketch(params: SketchParams,
                  hashFunction: LSHFunction[Signature[Any]],
                  N: Int,
                  prototypesByOrigin: RDD[(Index, DataPoint)])