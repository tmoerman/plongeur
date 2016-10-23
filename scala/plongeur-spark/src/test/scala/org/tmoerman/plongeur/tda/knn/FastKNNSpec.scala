package org.tmoerman.plongeur.tda.knn

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model.{DataPoint, TDAContext}
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.tda.knn.FastKNN._
import org.tmoerman.plongeur.tda.knn.KNN._
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

    assertDistanceFrequencies(acc)
  }

  it should "yield correct frequencies for combined partition accumulators" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val (a, b) = points.splitAt(4)
    val acc = union(bruteForceAcc(a), bruteForceAcc(b))

    assertDistanceFrequencies(acc)
  }

  it should "yield correct frequencies for the sparse matrix" in {
    implicit val d: DistanceFunction = EuclideanDistance

    val acc = bruteForceAcc(points)
    val sparse = toSparseMatrix(points.size, acc)

    assertDistanceFrequenciesM(sparse)
  }

  private def bruteForceAcc(points: Seq[DataPoint])(implicit k: Int = 2, d: DistanceFunction): ACC =
    (points: @unchecked) match {
      case x :: xs => xs.foldLeft(init(x))(concat)
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

  lazy val exactEuclidean = ExactKNN.exactACC(ctx, ExactKNNParams(k = k, distance = EuclideanDistance))

  val lshParamsEuclidean = LSHParams(
    signatureLength = 10,
    radius = Some(LSH.estimateRadius(ctx)),
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

  lazy val exactCosine = ExactKNN.exactACC(ctx, ExactKNNParams(k = k, distance = CosineDistance))

  it should "yield identical results with the same seed value" in {
    assertEqualResultsForEqualSeed(fastParamsCosine, exactCosine)
  }

  it should "yield increasing accuracy with increasing L" in {
    assertIncreasingAccuracy(fastParamsCosine, exactCosine)
  }

  private def assertEqualResultsForEqualSeed(fastParams: FastKNNParams, baseLine: ACC): Unit = {
    val a = FastKNN.fastACC(ctx, fastParams)
    val b = FastKNN.fastACC(ctx, fastParams)

    val x = KNN.accuracy(a, baseLine)
    val y = KNN.accuracy(b, baseLine)

    x shouldBe y
  }

  private def assertIncreasingAccuracy(fastParams: FastKNNParams, baseLine: ACC): Unit = {
    val accuracies =
      (1 to 5).map(L => {
        val newParams = fastParams.copy(nrHashTables = L)

        val fastACC = FastKNN.fastACC(ctx, newParams)

        KNN.accuracy(fastACC, baseLine)
      })

    accuracies.sliding(2, 1).foreach{ case Seq(a, b) => {
      a should be <= b
    }}
  }

}