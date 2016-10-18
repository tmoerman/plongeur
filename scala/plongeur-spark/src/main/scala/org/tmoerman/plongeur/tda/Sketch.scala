package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.lang.Math.{min, pow, sqrt}
import java.util.{Random => JavaRandom}

import breeze.linalg.DenseVector.fill
import com.github.karlhigley.spark.neighbors.lsh.{LSHFunction, Signature}
import org.apache.spark.RangePartitioner
import org.apache.spark.mllib.linalg.VectorConversions._
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH.{LSHParams, toArray}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.Sketch.SketchParams

import scala.annotation.tailrec
import scala.util.Random._
import scala.util.hashing.MurmurHash3.arrayHash

/**
  * @author Thomas Moerman
  */
object Sketch extends Serializable {

  type HashKey = Int

  /**
    * @param lshParams
    * @param prototypeStrategy The strategy for collapsing colliding points into a prototype point.
    */
  case class SketchParams(lshParams: LSHParams,
                          prototypeStrategy: PrototypeStrategy)

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
    * @param seed
    */
  case class RandomCandidate(implicit seed: Long) extends PrototypeStrategy {
    val random = new JavaRandom(seed)

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
    * @param seed A seed value.
    */
  case class ApproximateMedian(t: Int)(implicit seed: Long) extends PrototypeStrategy {
    val random: JavaRandom = new JavaRandom(seed)

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
    * @param ctx
    * @param sketchParams
    * @return Returns a Sketch of the data in the TDAContext with respect to the SketchParams.
    */
  def apply(ctx: TDAContext, sketchParams: SketchParams): Sketch = {
    import sketchParams._
    import lshParams._

    val nrPartitions = ctx.sc.defaultParallelism

    val hashFunctionTry = LSH.makeHashFunction(ctx.D, lshParams)

    def computeCollisionKey(point: DataPoint): HashKey =
      hashFunctionTry
        .map(_.signature(point.features))
        .flatMap(toArray)
        .map(arrayHash)
        .get // TODO propagate failure correctly

    val pointsByHashKey =
      ctx
        .dataPoints
        .map(point => (computeCollisionKey(point), point))
        .cache

    val partitionedByHashKey =
      pointsByHashKey
        .partitionBy(new RangePartitioner(nrPartitions, pointsByHashKey))

    val prototypes: RDD[(List[Index], DataPoint)] =
      prototypeStrategy
        .apply(partitionedByHashKey, distance)
        .zipWithUniqueId
        .map{ case ((ids, prototype: DataPoint), id: Long) => (ids, prototype.copy(index = id.toInt)) }
        .cache

    Sketch(sketchParams, hashFunctionTry.get, prototypes)
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

  lazy val dataPoints = prototypes.map(_._2).cache

  def frequencies =
    prototypes
      .map{ case (originalIds, p) => (p.index, originalIds.size) }
      .collect
      .sortBy(_._2)

  def lookupMap: Map[Index, Index] =
    prototypes
      .flatMap{ case (originalIds, p) => originalIds.map(id => (id, p.index)) }
      .collectAsMap
      .toMap

}