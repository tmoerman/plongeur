package org.tmoerman.plongeur.tda.geometry

import breeze.linalg.{CSCMatrix => BSM, DenseVector => BDV, SparseVector => BSV, Vector => BV}
import com.holdenkarau.spark.testing.SharedSparkContext
import org.apache.spark.mllib.linalg.{SparseMatrix, Vector => MLVector}
import org.apache.spark.rdd.RDD
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.geometry.Laplacian._
import org.tmoerman.plongeur.test.TestResources

/**
  * @author Thomas Moerman
  */
class LaplacianSpec extends FlatSpec with SharedSparkContext with TestResources with Matchers {

  import LaplacianSpec._

  implicit val distance = EuclideanDistance

  val sigma = 1.0

  behavior of "Laplacian"

  it should "should compute the laplacian embedding of an affinity matrix" in {
    val rdd = irisDataPointsRDD.cache

    val ctx = TDAContext(sc, rdd)

    val distancesRDD = distances(rdd)

    val affinitiesRDD = toAffinities(distancesRDD, sigma)

    val A = toSparseMatrix(ctx.N, affinitiesRDD.collect)

    val laplacian = Laplacian.fromAffinities(ctx, A)

    laplacian.first._2.size shouldBe rdd.first.features.size

    laplacian.count shouldBe ctx.N
  }

  "computing a vectorized laplacian for tanimoto distance" should "work" in {
    import org.apache.spark.mllib.linalg.BreezeConversions._

    val N = 5

    val rawFeatures: Array[BSV[Double]] = Array(
      BSV(N)((0, 1d), (2, 1d), (4, 1d)),
      BSV(N)((1, 1d), (3, 1d), (4, 1d)),
      BSV(N)((1, 1d), (2, 1d), (3, 1d)),
      BSV(N)((2, 1d), (3, 1d), (4, 1d)),
      BSV(N)((0, 1d), (1, 1d), (4, 1d)))

    // Turn the raw features into a sparse matrix
    val data: BSM[Double] =
      SparseMatrix
        .fromCOO(N, N,
          rawFeatures
            .view
            .zipWithIndex
            .flatMap{ case (v, rowIdx) => v.activeIterator.map{ case (colIdx, v) => (rowIdx, colIdx, v) }})
        .toBreeze
        .asInstanceOf[BSM[Double]]

    val IPM = data * data.t

    val W = SparseMatrix.fromCOO(N, N, for {
      i <- (0 to N-1)
      j <- (0 to N-1)
      if i != j // 0 diagonal
      d <- {
        val dotp = IPM(i, j)
        val denom = IPM(i, i) + IPM(j, j) - dotp
        val tanimotoDistance = if (denom == 0d) None else Some(1 - (dotp / denom))

        tanimotoDistance
      }
    } yield (i, j, gaussianSimilarity(d))).toBreeze.asInstanceOf[BSM[Double]]

    //val Degrees = sum(W(*, ::))

    //Degrees

//    val Dv_pow: BDV[Double] = Degrees.toDenseVector :^ -0.5d
//
//    val D_pow = diag(Dv_pow)
//
//    val L = D_pow * W * D_pow
//
//    println(L)
  }

}

object LaplacianSpec {

  type Triplet = (Index, Index, Distance)

  def toSparseMatrix(N: Int, triplets: Iterable[Triplet]) = SparseMatrix.fromCOO(N, N, triplets)

  def toAffinities(distancesRDD: RDD[Triplet], sigma: Double): RDD[Triplet] =
    distancesRDD
      .map{ case (p, q, d) => (p, q, gaussianSimilarity(d, sigma)) }
      .cache

  def distances(rdd: RDD[DataPoint])(implicit distance: DistanceFunction): RDD[Triplet] =
    (rdd cartesian rdd)
      .filter{ case (p, q) => (p.index != q.index) }
      .map{ case (p, q) => (p.index, q.index, distance(p, q)) }.cache

}