package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.lang.Math.{min, pow, sqrt}
import java.util.{Random => JavaRandom}

import breeze.linalg.DenseVector.fill
import com.github.karlhigley.spark.neighbors.lsh.{LSHFunction, ScalarRandomProjectionFunction, SignRandomProjectionFunction, Signature}
import org.apache.spark.mllib.linalg.VectorConversions._
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch.SketchParams

import scala.annotation.tailrec
import scala.collection.BitSet
import scala.util.Random._
import scala.util.hashing.MurmurHash3

/**
  * @author Thomas Moerman
  */
object Sketch extends Serializable {

  type SignatureLength = Int
  type Radius = Double
  type HashKey = Serializable
  type Count = Int

  /**
    * @param k The signature length.
    * @param r The radius.
    * @param distance The distance function, necessary for choosing the hashing strategy.
    * @param prototypeStrategy The strategy for collapsing colliding points into a prototype point.
    */
  case class SketchParams(k: SignatureLength,
                          r: Radius,
                          prototypeStrategy: PrototypeStrategy,
                          distance: DistanceFunction = DEFAULT)

  def estimateRadius(ctx: TDAContext): Radius = ???

  /**
    * Abstract type describing the strategy to choose a representant (a.k.a. Prototype) of a collection of DataPoints
    * that collide in the same hash bucket.
    */
  trait PrototypeStrategy extends SimpleName with Serializable {

    /**
      * @param rdd
      * @param distance
      * @return Returns an RDD of DataPoints and
      */
    def apply(rdd: RDD[(HashKey, DataPoint)], distance: DistanceFunction): RDD[(List[Index], DataPoint)]

  }

  /**
    * PrototypeStrategy that reduces to a random data point per key.
    *
    * @param random
    */
  case class RandomCandidate(random: JavaRandom = new JavaRandom) extends PrototypeStrategy {

    type ACC = (List[Index], DataPoint)

    def init(d: DataPoint): ACC = (d.index :: Nil, d)

    def concat(acc: ACC, d: DataPoint): ACC = acc match {
      case (indices, proto) => (d.index :: indices, if (random.nextBoolean) proto else d)
    }

    def merge(acc1: ACC, acc2: ACC): ACC = (acc1, acc2) match {
      case ((indices1, proto1), (indices2, proto2)) => (indices1 ::: indices2, if (random.nextBoolean) proto1 else proto2)
    }

    override def apply(pointsByHashKey: RDD[(HashKey, DataPoint)],
                       distance: DistanceFunction = DEFAULT) =
      pointsByHashKey
        .combineByKey(init, concat, merge)
        .values

  }

  /**
    * PrototypeStrategy that reduces to the arithmetic mean (center of gravity) data point per key.
    * It is possible (and likely) that the arithmetic mean is not a member of the input data.
    */
  case object ArithmeticMean extends PrototypeStrategy {

    type ACC = (List[Index], Count, DataPoint)

    def init(d: DataPoint): ACC = (d.index :: Nil, 1, d)

    def sum(d1: DataPoint, d2: DataPoint) = DataPoint(-1, (d1.features.toBreeze + d2.features.toBreeze).toMLLib)

    def div(d: DataPoint, scalar: Int) = DataPoint(-1, (d.features.toBreeze / fill(d.features.size, scalar.toDouble)).toMLLib)

    def concat(acc: ACC, d: DataPoint): ACC = acc match {
      case (indices, count, proto) =>
        (d.index :: indices, count + 1, sum(proto, d))
    }

    def merge(acc1: ACC, acc2: ACC): ACC = (acc1, acc2) match {
      case ((indices1, count1, proto1), (indices2, count2, proto2)) => (indices1 ::: indices2, count1 + count2, sum(proto1, proto2))
    }

    override def apply(pointsByHashKey: RDD[(HashKey, DataPoint)],
                       distance: DistanceFunction = DEFAULT) =
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
    * @param random A random generator.
    */
  case class ApproximateMedian(t: Int, random: JavaRandom = new JavaRandom) extends PrototypeStrategy {

    def exactWinner(distance: DistanceFunction)(S: Iterable[DataPoint]): DataPoint =
      S
        .map(a => (a, S.filter(b => b != a).map(b => distance(a,b)).sum))
        .minBy(_._2)
        ._1

    def mergeTail(l: List[List[DataPoint]]) = l.reverse match {
      case list @ (x :: y :: rest) => if (x.size < t) (x ++ y) :: rest else list
      case list => list
    }

    def approximateMedian(S: List[DataPoint], distance: DistanceFunction): DataPoint = {
      val threshold = min(pow(t, 2) - 1, sqrt(S.size)).toInt

      val winner = exactWinner(distance) _

      @tailrec def recur(currS: List[DataPoint]): DataPoint =
        if (currS.size <= threshold)
          winner(currS)
        else {
          val grouped = random.shuffle(currS).grouped(t).toList

          recur(mergeTail(grouped).map(winner))
        }

      recur(S)
    }

    override def apply(pointsByHashKey: RDD[(HashKey, DataPoint)],
                       distance: DistanceFunction = DEFAULT) =
      pointsByHashKey
        .groupByKey
        .map{ case (key, candidatePoints) =>
          val indices = candidatePoints.map(_.index).toList

          val prototype = approximateMedian(candidatePoints.toList, distance)

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

    lazy val random = new JavaRandom

    distance match {
      case CosineDistance      => SignRandomProjectionFunction.generate(d, k, random)
      case EuclideanDistance   => ScalarRandomProjectionFunction.generateL2(d, k, r, random)
      case ManhattanDistance   => ScalarRandomProjectionFunction.generateL1(d, k, r, random)
      case LpNormDistance(0.5) => ScalarRandomProjectionFunction.generateFractional(d, k, r, random)

      case _ => throw new IllegalArgumentException(s"No sketch available for distance function: $distance")
    }
  }

  /**
    * @param ctx
    * @param params
    * @return Returns a Sketch of the data in the TDAContext with respect to the SketchParams.
    */
  def apply(ctx: TDAContext, params: SketchParams): Sketch = {
    import params._

    val hashFunction = makeHashFunction(ctx.D, params)

    def computeCollisionKey(point: DataPoint): HashKey = {
      val sig: Signature[_] = hashFunction.signature(point.features)

      val array: Array[Int] = sig.elements match {
        case b: BitSet     => b.toArray
        case a: Array[Int] => a
        case _ => throw new IllegalArgumentException("Cannot convert signature into Array[Int]")
      }

      MurmurHash3.arrayHash(array)
    }

    val pointsByHashKey =
      ctx
        .dataPoints
        .map(point => (computeCollisionKey(point), point))

    val prototypes: RDD[(List[Index], DataPoint)] =
      prototypeStrategy
        .apply(pointsByHashKey, distance)
        .zipWithUniqueId
        .map{ case ((ids, prototype: DataPoint), id: Long) => (ids, prototype.copy(index = id.toInt)) }
        .cache

    Sketch(params, hashFunction, prototypes)
  }

}

/**
  * @param params The SketchParams that were used to create this Sketch.
  * @param hashFunction The randomly selected LSH hash function.
  * @param prototypes An RDD keyed by the list of indices of the original DataPoint instances to (non-unique)
  *                   collapsed prototypes.
  */
case class Sketch(params: SketchParams,
                  hashFunction: LSHFunction[Signature[Any]],
                  prototypes: RDD[(List[Index], DataPoint)]) extends ContextLike with Serializable {

  lazy val N = prototypes.count.toInt

  lazy val D = prototypes.first._2.features.size

  lazy val frequencies = prototypes.map(_._1.size).collect.toList

  lazy val dataPoints = prototypes.map(_._2).cache

  override def toString = s"Sketch(N=$N)"

  def toBroadcastValue = prototypes.collectAsMap

  lazy val originLookup = prototypes.map{ case (ids, p) => (p.index, ids) }.collectAsMap

  //  lazy val prototypesByOrigin =
  //    indexedPrototypes
  //      .flatMap{ case (ids, prototype: IndexedDataPoint) => ids.map(id => (id, prototype)) }
  //      .cache

}