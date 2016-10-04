package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.Random

import breeze.linalg.DenseVector.fill
import com.github.karlhigley.spark.neighbors.lsh.{ScalarRandomProjectionFunction, SignRandomProjectionFunction}
import org.apache.spark.mllib.linalg.VectorConversions._
import org.apache.spark.mllib.linalg.{Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance._
import org.tmoerman.plongeur.tda.Model.{SimpleName, DataPoint, Index, TDAContext}
import org.tmoerman.plongeur.tda.Sketch.{SketchParams, Prototype}

import scala.util.hashing.MurmurHash3

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

  case class ApproximateMedian(random: Random = new Random()) extends PrototypeStrategy {

    def winner(set: Set[DataPoint]): DataPoint = ???

    def approximateMedian(set: Set[DataPoint]): DataPoint = ???

    override def apply(pointsByHashKey: RDD[(Any, DataPoint)]) =
      pointsByHashKey
        .groupByKey
        .map{ case (key, candidatePoints) =>
          val indices = candidatePoints.map(_.index).toList

          val prototype = approximateMedian(candidatePoints.toSet)

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

    Sketch(params, N, prototypesByOrigin)
  }

}

/**
  * @param params The SketchParams that were used to create this Sketch.
  * @param N The number of prototypes. Note: this is not equal to the size of the RDD.
  * @param prototypesByOrigin An RDD keyed by the indices of the original DataPoint instances to (non-unique)
  *                           collapsed prototypes.
  */
case class Sketch(params: SketchParams,
                  N: Int,
                  prototypesByOrigin: RDD[(Index, DataPoint)])