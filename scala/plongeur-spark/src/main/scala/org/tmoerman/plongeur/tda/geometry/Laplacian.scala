package org.tmoerman.plongeur.tda.geometry

import java.lang.Math.{min, exp, pow}

import breeze.linalg.DenseVector._
import breeze.linalg.{*, sum, diag, Matrix => BM, SparseVector => BSV, CSCMatrix => BSM, DenseVector => BDV}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.apache.spark.mllib.linalg.distributed.{RowMatrix, IndexedRow, IndexedRowMatrix}
import org.apache.spark.mllib.linalg.{SparseMatrix, Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances.{TanimotoDistance, DistanceFunction, Distance}
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

  val NR_EIGENVECTORS = 10

  /**
    * Here we use the version proposed in "On Spectral Clustering" -- Ng et al. 2003
    *
    * @param ctx
    * @param rdd
    * @param k
    * @param sigma
    * @return Returns the Laplacian as a RowMatrix in function of the specified KNN data structure.
    */
  def apply(ctx: TDAContext, rdd: KNN_RDD, k: Option[Int] = None, sigma: Double = DEFAULT_SIGMA): RDD[(Index, MLVector)] = {
    val N = ctx.N

    val topK: KNN_RDD_Like = rdd.mapValues(bpq => k.map(K => bpq.take(min(K, bpq.size))).getOrElse(bpq))

    val affinityRDD = toAffinities(topK, sigma)

    val A = toSparseMatrix(N, affinityRDD)

    fromAffinities(ctx, A, sigma)
  }

  /**
    * @param ctx
    * @param gaussianAffinities
    * @return Returns the normalized Laplacian as a RowMatrix in function of the specified affinity matrix.
    */
  def fromAffinities(ctx: TDAContext, gaussianAffinities: SparseMatrix, sigma: Double = DEFAULT_SIGMA): RDD[(Index, MLVector)] = {
    val D_pow = degrees(gaussianAffinities, -0.5).toBreeze

    val W = gaussianAffinities.toBreeze

    val L = D_pow * W * D_pow

    normalizedEigenvectors(ctx, L)
  }

  def normalizedEigenvectors(ctx: TDAContext, L: BM[Distance]): RDD[(Index, MLVector)] = {
    val svd = toIndexedRowMatrix(ctx.sc, L).computeSVD(min(ctx.D, NR_EIGENVECTORS), computeU = true)

    val normalizer = new Normalizer()

    val U_normalized = svd.U.rows.map(r => (r.index.toInt, normalizer.transform(r.vector)))

    U_normalized
  }

  /**
    * A Breeze implementation.
    *
    * @param ctx
    * @param sigma
    * @return Returns the normalized Laplacian as a RowMatrix. A vectorized implementation of the Tanimoto distance
    *         is used to compute the distance matrix.
    */
  @deprecated("experimental") def tanimotoBreeze(ctx: TDAContext, sigma: Double = DEFAULT_SIGMA): RDD[(Index, MLVector)] = {
    val N = ctx.N
    val D = ctx.D

    val rawFeatures = ctx.dataPoints.map(_.features.toBreeze).collect

    val data: BSM[Double] =
      SparseMatrix
        .fromCOO(N, D,
          rawFeatures
            .view
            .zipWithIndex
            .flatMap{ case (v, rowIdx) => v.activeIterator.map{ case (colIdx, v) => (rowIdx, colIdx, v) }})
        .toBreeze
        .asInstanceOf[BSM[Double]]

    val IPM = data * data.t // inner product matrix

    def lookup(i: Int, j: Int) = IPM(i,j)

    val W = SparseMatrix.fromCOO(N, N, for {
      i <- (0 to N-1)
      j <- (0 to N-1)
      if i != j // 0s on the diagonal
      d <- {
        val dotp = lookup(i, j)
        val denom = lookup(i, i) + lookup(j, j) - dotp
        val tanimotoDistance = if (denom == 0d) None else Some(1 - (dotp / denom))

        tanimotoDistance
      }
    } yield (i, j, gaussianSimilarity(d))).toBreeze.asInstanceOf[BSM[Double]]

    val Degrees = W * ones[Double](N)

    val Dv_pow = Degrees :^ -0.5d

    val D_pow = diag(BSV(Dv_pow.toArray))

    val L = D_pow * W * D_pow

    normalizedEigenvectors(ctx, L)
  }

  /**
    * A Spark implementation.
    *
    * @param ctx
    * @param sigma
    * @return Returns the normalized Laplacian as a RowMatrix. A vectorized implementation of the Tanimoto distance
    *         is used to compute the distance matrix.
    */
  def tanimoto(ctx: TDAContext, sigma: Double = DEFAULT_SIGMA): RDD[(Index, MLVector)] = {
    val N = ctx.N
    val D = ctx.D

    val rawFeatures = ctx.dataPoints.map(_.features.toBreeze).collect

    val featuresRDD = ctx.dataPoints.map(_.features).cache

    val rowMatrix = new RowMatrix(featuresRDD)

    val dataMatrix =
      SparseMatrix
        .fromCOO(N, D,
          rawFeatures
            .view
            .zipWithIndex
            .flatMap{ case (v, rowIdx) => v.activeIterator.map{ case (colIdx, v) => (rowIdx, colIdx, v) }})

    val IPM = rowMatrix.multiply(dataMatrix.toDense.transpose)

    val C = IPM.rows.map(_.vector).collect

    def lookup(i: Int, j: Int): Double = C(i)(j)

    val W = SparseMatrix.fromCOO(N, N, for {
      i <- (0 to N-1)
      j <- (0 to N-1)
      if i != j // 0s on the diagonal
      d <- {
        val dotp = lookup(i, j)
        val denom = lookup(i, i) + lookup(j, j) - dotp
        val tanimotoDistance = if (denom == 0d) None else Some(1 - (dotp / denom))

        tanimotoDistance
      }
    } yield (i, j, gaussianSimilarity(d))).toBreeze.asInstanceOf[BSM[Double]]

    val Degrees = W * ones[Double](N)

    val Dv_pow = Degrees :^ -0.5d

    val D_pow = diag(BSV(Dv_pow.toArray))

    val L = D_pow * W * D_pow

    normalizedEigenvectors(ctx, L)
  }

  /**
    * @param sc
    * @param m
    * @return Returns the specified matrix transformed into a Spark RowMatrix.
    */
  def toIndexedRowMatrix(sc: SparkContext, m: BM[Distance]): IndexedRowMatrix = {
    val LRowVectors = m.toMLLib.rowVectors

    val indexedRows =
      sc
        .parallelize(LRowVectors.toSeq)
        .zipWithIndex
        .map { case (v, idx) => IndexedRow(idx, v) }
        .cache

    new IndexedRowMatrix(indexedRows)
  }

  /**
    * @param rdd
    * @param sigma
    * @return Returns an affinity RDD in function of the distance RDD.
    */
  def toAffinities(rdd: KNN_RDD_Like, sigma: Double): KNN_RDD_Like =
    rdd.map { case (p, bpq) => (p, bpq.map { case (q, d) => (q, gaussianSimilarity(d, sigma)) }) }

  /**
    * @param sigma
    * @param d
    * @return Returns $\exp\left(-\left(\frac{d}{\sigma}\right)^2\right)$
    */
  def gaussianSimilarity(d: Distance, sigma: Double = DEFAULT_SIGMA) = exp(- pow(d, 2) / (2 * sigma))

  /**
    * @param m
    * @return Returns the ML Vector of degrees of the specified SparseMatrix.
    */
  def degrees(m: SparseMatrix, exp: Double) = {
    // TODO vectorize this calculation

    val diagonalValues = m.rowVectors.map(_.toArray.sum).map(pow(_, exp))

    SparseMatrix.fromCOO(m.numRows, m.numCols, diagonalValues.zipWithIndex.map{ case (v, i) => (i, i, v) }.toIterable)
  }

}