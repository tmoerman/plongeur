package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.{Random => JavaRandom}

import breeze.linalg.{DenseVector => BDV, SparseVector => BSV, Vector => BV, _}
import com.github.karlhigley.spark.neighbors.lsh.{LSHFunction, ScalarRandomProjectionFunction, SignRandomProjectionFunction, Signature}
import org.apache.spark.mllib.linalg.VectorConversions._
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model.TDAContext

import scala.collection.BitSet
import scala.util.Random.nextLong
import scala.util.Try

/**
  * LSH related functions.
  *
  * @author Thomas Moerman
  */
object LSH extends Serializable {

  type SignatureLength = Int
  type Radius = Double

  /**
    * Parameters for LSH functions.
    *
    * @param signatureLength Also known as the k parameter, cfr. LSH literature.
    * @param radius
    * @param distance The distance function.
    * @param seed Implicit random seed.
    */
  case class LSHParams(signatureLength: SignatureLength,
                       radius: Option[Radius] = None,
                       distance: DistanceFunction = DEFAULT,
                       seed: Long = nextLong)

  /**
    * @param d The original dimensionality of the data points.
    * @param params
    * @return Returns a random projection LSH function.
    */
  def makeHashFunction(d: Int, params: LSHParams): Try[LSHFunction[Signature[_]]] = {
    import params._

    lazy val random = new JavaRandom(seed)
    lazy val r = radius.get

    Try(distance match {
      case CosineDistance      => SignRandomProjectionFunction   generate           (d, signatureLength, random)
      case EuclideanDistance   => ScalarRandomProjectionFunction generateL2         (d, signatureLength, r, random)
      case ManhattanDistance   => ScalarRandomProjectionFunction generateL1         (d, signatureLength, r, random)
      case LpNormDistance(0.5) => ScalarRandomProjectionFunction generateFractional (d, signatureLength, r, random)

      case _ => throw new IllegalArgumentException(s"No hash function available for distance function: $distance")
    })
  }

  /**
    * @param signature
    * @return Returns a Try of converting the signature into an Array[Int].
    */
  def toArray(signature: Signature[_]): Array[Int] = signature.elements match {
    case a: Array[Int] => a
    case b: BitSet     => b.toArray
    case _             => throw new UnsupportedOperationException(s"Cannot convert $signature to Array[Int]")
  }

  /**
    * @param signature
    * @return Returns a Try of the signature cast to a breeze DenseVector
    */
  def toVector(length: Int, signature: Signature[_]): BV[Double] = signature.elements match {
    case a: Array[Int] => BDV(a.map(_.toDouble))
    case b: BitSet     => BSV(length)(b.map(index => (index, 1.0)).toSeq: _*)
    case _             => throw new UnsupportedOperationException(s"Cannot convert $signature to breeze Vector[Double]")
  }

  /**
    * @param ctx
    * @return Returns the maximum radius of the hypercube spanned by the observations.
    */
  def maxRadius(ctx: TDAContext) = {
    import ctx.stats

    val max = stats.max.toBreeze
    val min = stats.min.toBreeze
    val delta = max - min

    delta(argmax(delta))
  }

  /**
    * @param ctx
    * @param fraction
    * @return Returns an estimate for the radius of the $L_p$ LSH function radius.
    */
  def estimateRadius(ctx: TDAContext, fraction: Double = 1d / 25): Radius = maxRadius(ctx) * fraction

}