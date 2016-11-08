package org.tmoerman.plongeur.tda.geometry

import java.lang.Math.{exp, pow}

import breeze.linalg._
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.{SparseMatrix => MLSparseMatrix, Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances.Distance
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.{KNN_RDD_Set, _}
import org.tmoerman.plongeur.util.MatrixFunctions._

/**
  * See:
  *
  *   1. "A Tutorial on Spectral Clustering"
  *     -- Ulrike von Luxburg
  *
  *   2. "On Spectral Clustering"
  *     -- Andrew Y. Ng et al. 2003
  *
  *   2. "megaman: Manifold Learning with Millions of points"
  *     -- VanderPlas et al.
  *
  * @author Thomas Moerman
  */
object Laplacian {

  val DEFAULT_SIGMA = 1.0d

  sealed trait Variant {
    // def apply(weights: SparseMatrix)
  }

//  case object Unnormalized        extends Variant
//  case object SymmetricNormalized extends Variant
//  case object RandomWalk          extends Variant
//  case object Geometric           extends Variant

  case class LaplacianParams(normalized: Boolean = true,
                             sigma:      Double  = DEFAULT_SIGMA) extends Serializable
                             // variant:    Variant = Geometric


  /**
    * Here we use the version proposed in "On Spectral Clustering" -- Ng et al. 2003
    *
    * @param ctx
    * @param rdd
    * @param params
    * @return
    */
  def toLaplacian(ctx: TDAContext, rdd: KNN_RDD_Set, params: LaplacianParams): RowMatrix = {
    import params._

    val N = ctx.N

    val affinityRDD = toAffinities(rdd, sigma)

    val A = toSparseMatrix(N, affinityRDD)

    val Dpow = degrees(A, -0.5).toBreeze

    val W = A.toBreeze

    val L = Dpow :* W :* Dpow

//    val L_RDD: RowMatrix = new RowMatrix(ctx.sc.parallelize(L.toMLLib.asInstanceOf[MLSparseMatrix].rowVectors.toSeq))
//
//    val svd = L_RDD.computeSVD(3, computeU = true)
//
//    svd.U

    ???
  }

  /**
    * @param rdd
    * @param sigma
    * @return Returns an affinity RDD in function of the distance RDD.
    */
  def toAffinities(rdd: KNN_RDD_Set, sigma: Double): RDD[(Index, Set[(Index, Distance)])] =
    rdd.map { case (p, set) => (p, set.map { case (q, d) => (q, gaussianSimilarity(d, sigma)) }) }

  /**
    * @param sigma
    * @param d
    * @return Returns $\exp\left(-\left(\frac{d}{\sigma}\right)^2\right)$
    */
  def gaussianSimilarity(d: Distance, sigma: Double = DEFAULT_SIGMA) = exp(-pow(d / (2 * sigma), 2))

  /**
    * @param m
    * @return Returns the ML Vector of degrees of the specified SparseMatrix.
    */
  def degrees(m: MLSparseMatrix, exp: Double) = {
    val diagonalValues = m.rowVectors.map(_.values.sum).map(sum => pow(sum, exp))

    MLSparseMatrix.fromCOO(m.numRows, m.numCols, diagonalValues.zipWithIndex.map{ case (v, i) => (i, i, v) }.toIterable)
  }


}