package org.tmoerman.lab

import breeze.linalg.*
import org.apache.spark.mllib.linalg.BreezeConversions._
import org.apache.spark.mllib.linalg.{SparseVector, SparseMatrix, Vector => MLVector}
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.rdd.RDD
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.{Distance, DistanceFunction, EuclideanDistance}
import org.tmoerman.plongeur.tda.Model.{DataPoint, Index}
import org.tmoerman.plongeur.tda.geometry.Laplacian._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}
import org.tmoerman.plongeur.util.MatrixFunctions._

/**
  * @author Thomas Moerman
  */
class LaplacianLab extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  import LaplacianLab._

  behavior of "Laplacian Lab"

  implicit val distance = EuclideanDistance

  val sigma = 1.0

  val rdd = irisDataPointsRDD.cache

  val N = rdd.count.toInt

  val distancesRDD = distances(rdd)

  val affinitiesRDD = affinities(distancesRDD, sigma)

  it should "print stats" in {
    println("Iris data set")

    println(s"nr of points: ${rdd.count}")

    println(s"distances top 10: ${distancesRDD.take(10).mkString(", ")}")

    println(s"affinities top 10: ${affinitiesRDD.take(10).mkString(", ")}")
  }

  it should "compute Laplacian eigenvectors" in {
    val A = toMatrix(N, affinitiesRDD.collect)

    val r = 0 to 2

    val Dpow = degrees(A, -0.5).toBreeze

    val W = A.toBreeze

    val L = Dpow * W * Dpow

    val rowVectors: Iterator[MLVector] = L.toMLLib.asInstanceOf[SparseMatrix].rowVectors

    val rowsRDD = sc.parallelize(rowVectors.toSeq).cache

    val L_RDD = new RowMatrix(rowsRDD)

    val svd = L_RDD.computeSVD(3, computeU = true)

    val Urows = svd.U.rows.collect

    println("\nD^{-1/2} = \n" + Dpow(r, r))
    println("\nW = \n" + W(r, r))
    println("\nL = \n" + L(r, r))
    println("\nU = \n" + svd.U.rows.take(10).mkString("\n"))
    println("\nV = \n" + svd.V.toBreeze(0 to 9, r))
    
    Urows.size shouldBe N
  }

}

object LaplacianLab {

  type Triplet = (Index, Index, Distance)

  def toMatrix(N: Int, triplets: Iterable[Triplet]) = SparseMatrix.fromCOO(N, N, triplets)

  def affinities(distancesRDD: RDD[Triplet], sigma: Double): RDD[Triplet] =
    distancesRDD
      .map{ case (p, q, d) => (p, q, gaussianSimilarity(d, sigma)) }
      .cache

  def distances(rdd: RDD[DataPoint])(implicit distance: DistanceFunction): RDD[Triplet] =
    (rdd cartesian rdd)
      .filter{ case (p, q) => (p.index != q.index) }
      .map{ case (p, q) => (p.index, q.index, distance(p, q)) }.cache

}