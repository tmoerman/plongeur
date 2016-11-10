package org.tmoerman.plongeur.tda.geometry

import java.lang.Math.{exp, pow}

import breeze.linalg.Matrix
import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.{SparseMatrix, Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances.Distance
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn._
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
  *   3. "megaman: Manifold Learning with Millions of points"
  *     -- VanderPlas et al.
  *
  * @author Thomas Moerman
  */
object Laplacian {

  val DEFAULT_SIGMA = 1.0d

  /**
    * @param normalized
    * @param nrEigenVectors
    * @param sigma
    */
  case class LaplacianParams(normalized: Boolean = true,
                             nrEigenVectors: Int = 2,
                             sigma: Double       = DEFAULT_SIGMA) extends Serializable

  /**
    * Here we use the version proposed in "On Spectral Clustering" -- Ng et al. 2003
    *
    * @param ctx
    * @param rdd
    * @param params
    * @return Returns the Laplacian as a RowMatrix in function of the specified KNN data structure.
    */
  def apply(ctx: TDAContext, rdd: KNN_RDD, params: LaplacianParams): RowMatrix = {
    import params._

    val N = ctx.N

    val affinityRDD = toAffinities(rdd, sigma)

    val A = toSparseMatrix(N, affinityRDD)

    fromAffinities(ctx, A, params)
  }

  /**
    * @param ctx
    * @param A
    * @param params
    * @return Returns the Laplacian as a RowMatrix in function of the specified affinity matrix.
    */
  def fromAffinities(ctx: TDAContext, A: SparseMatrix, params: LaplacianParams): RowMatrix = {
    import params._

    val Dpow = degrees(A, -0.5).toBreeze

    val W = A.toBreeze

    val L = Dpow * W * Dpow

    val svd = toRowMatrix(ctx.sc, L).computeSVD(nrEigenVectors, computeU = true)

    val normalizer = new Normalizer()

    val UNormalized = svd.U.rows.map(normalizer.transform(_)).cache

    new RowMatrix(UNormalized)
  }

  /**
    * @param sc
    * @param m
    * @return Returns the specified matrix transformed into a Spark RowMatrix.
    */
  def toRowMatrix(sc: SparkContext, m: Matrix[Distance]): RowMatrix = {
    val LRowVectors = m.toMLLib.asInstanceOf[SparseMatrix].rowVectors

    new RowMatrix(sc.parallelize(LRowVectors.toSeq).cache)
  }

  /**
    * @param rdd
    * @param sigma
    * @return Returns an affinity RDD in function of the distance RDD.
    */
  def toAffinities(rdd: KNN_RDD, sigma: Double): KNN_RDD_Like =
    rdd.map { case (p, bpq) => (p, bpq.map { case (q, d) => (q, gaussianSimilarity(d, sigma)) }) }

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
  def degrees(m: SparseMatrix, exp: Double) = {
    val diagonalValues = m.rowVectors.map(_.toArray.sum).map(pow(_, exp))

    SparseMatrix.fromCOO(m.numRows, m.numCols, diagonalValues.zipWithIndex.map{ case (v, i) => (i, i, v) }.toIterable)
  }

}