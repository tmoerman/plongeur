package org.tmoerman.plongeur.tda.geometry

import org.apache.spark.mllib.linalg.SparseMatrix
import org.apache.spark.rdd.RDD
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.geometry.Laplacian._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}

/**
  * @author Thomas Moerman
  */
class LaplacianSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  import LaplacianSpec._

  implicit val distance = EuclideanDistance

  val sigma = 1.0

  val rdd = irisDataPointsRDD.cache

  val ctx = TDAContext(sc, rdd)

  val N = rdd.count.toInt

  val distancesRDD = distances(rdd)

  val affinitiesRDD = toAffinities(distancesRDD, sigma)

  behavior of "Laplacian"

  it should "should compute the laplacian embedding of an affinity matrix" in {
    val A = toSparseMatrix(N, affinitiesRDD.collect)

    val laplacian = Laplacian.fromAffinities(ctx, A)

    laplacian.first._2.size shouldBe rdd.first.features.size

    laplacian.count shouldBe N
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