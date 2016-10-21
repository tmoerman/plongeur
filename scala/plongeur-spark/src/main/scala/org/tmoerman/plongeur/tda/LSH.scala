package org.tmoerman.plongeur.tda

import java.io.Serializable
import java.util.{Random => JavaRandom}

import breeze.linalg.{DenseVector => BDV, _}
import com.github.karlhigley.spark.neighbors.lsh.{LSHFunction, ScalarRandomProjectionFunction, SignRandomProjectionFunction, Signature}
import org.apache.spark.mllib.linalg.VectorConversions._
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model.TDAContext

import scala.collection.BitSet
import scala.util.Random.nextLong
import scala.util.{Failure, Success, Try}

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
                       distance: DistanceFunction = DEFAULT)
                      (implicit val seed: Long = nextLong) extends Serializable

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
  def toArray(signature: Signature[_]): Try[Array[Int]] = signature.elements match {
    case b: BitSet     => Success(b.toArray)
    case a: Array[Int] => Success(a)
    case _             => Failure(new IllegalArgumentException("Cannot convert signature into Array[Int]"))
  }

  /**
    * @param signature
    * @return Returns a Try of the signature cast to a breeze DenseVector
    */
  def toVector(signature: Signature[_]): BDV[Double] =
    toArray(signature).map(_.map(_.toDouble)).map(BDV(_)).get

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