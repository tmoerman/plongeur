package org.tmoerman.plongeur.tda.TestCommons

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model.{DataPoint, TDAContext}
import org.tmoerman.plongeur.tda.knn.Commons._
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.tda.knn.FastKNN._
import org.tmoerman.plongeur.tda.knn._
import org.tmoerman.plongeur.tda.knn.{ExactKNN, FastKNN}
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}
import org.tmoerman.plongeur.util.MatrixFunctions._

/**
  * @author Thomas Moerman
  */
class FastKNNSpec extends FlatSpec with SparkContextSpec with Matchers with TestResources {

  behavior of "brute force kNN functions"

  it should "yield correct frequencies for partition accumulators" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val acc = bruteForceAcc(points)

    assertDistanceFrequenciesAcc(acc)
  }

  it should "yield correct frequencies for combined partition accumulators" in {
    val N = points.size
    implicit val d = EuclideanDistance

    val (a, b) = points.splitAt(4)
    val acc = merge(N)(bruteForceAcc(a), bruteForceAcc(b))

    assertDistanceFrequenciesAcc(acc)
  }

  it should "yield correct frequencies for the sparse matrix" in {
    implicit val d = EuclideanDistance

    val acc = bruteForceAcc(points)

    val sparse = toSparseMatrix(points.size, acc)

    assertDistanceFrequenciesM(sparse)
  }

  private def bruteForceAcc(points: Seq[DataPoint])(implicit k: Int = 2, d: DistanceFunction): Accumulator =
    (points: @unchecked) match {
      case x :: xs => xs.foldLeft(init(k)(x))(concat(k))
    }

  behavior of "sparse matrix row iterator"

  it should "yield correct rows" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val acc    = bruteForceAcc(points)
    val sparse = toSparseMatrix(points.size, acc)
    val rows   = sparse.rowVectors.toList

    rows.size shouldBe 9
  }

  implicit val seed = 1L

  val k = 5
  
  lazy val ctx = TDAContext(sc, irisDataPointsRDD)

  behavior of "FastKNN with Euclidean distance"

  lazy val exactEuclidean = ExactKNN.apply(ctx, ExactKNNParams(k = k, distance = EuclideanDistance))

  val lshParamsEuclidean = LSHParams(
    signatureLength = 10,
    distance = EuclideanDistance,
    seed = seed)

  val fastParamsEuclidean = new FastKNNParams(
    k = k,
    blockSize = 20,
    nrHashTables = 1,
    lshParams = lshParamsEuclidean)

  it should "yield identical results with the same seed value" in {
    assertEqualResultsForEqualSeed(fastParamsEuclidean, exactEuclidean)
  }

  it should "yield increasing accuracy with increasing L" in {
    assertIncreasingAccuracy(fastParamsEuclidean, exactEuclidean)
  }

  behavior of "FastKNN with Cosine distance"

  val fastParamsCosine = fastParamsEuclidean.copy(lshParams = lshParamsEuclidean.copy(distance = CosineDistance))

  lazy val exactCosine = ExactKNN(ctx, ExactKNNParams(k = k, distance = CosineDistance))

  it should "yield identical results with the same seed value" in {
    assertEqualResultsForEqualSeed(fastParamsCosine, exactCosine)
  }

  it should "yield increasing accuracy with increasing L" in {
    assertIncreasingAccuracy(fastParamsCosine, exactCosine)
  }

  private def assertEqualResultsForEqualSeed(fastParams: FastKNNParams, baseLine: kNN_RDD): Unit = {
    val a = FastKNN.apply(ctx, fastParams)
    val b = FastKNN.apply(ctx, fastParams)

    val x = relativeAccuracy(a, baseLine)
    val y = relativeAccuracy(b, baseLine)

    x shouldBe y
  }

  private def assertIncreasingAccuracy(fastParams: FastKNNParams, baseLine: kNN_RDD): Unit = {
    val accuracies =
      (1 to 5).map(L => {
        val newParams = fastParams.copy(nrHashTables = L)

        val fastKNN = FastKNN(ctx, newParams)

        relativeAccuracy(fastKNN, baseLine)
      })

    accuracies.sliding(2, 1).foreach{ case Seq(a, b) => {
      println(s"$a < $b")

      a should be <= b
    }}
  }

}