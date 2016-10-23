package org.tmoerman.plongeur.tda.knn

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.{DistanceFunction, EuclideanDistance}
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

  behavior of "FastKNN"

  implicit val seed = 1L

  val k = 5
  val L = 1
  val params = new FastKNNParams(
    k = k,
    blockSize = 50,
    nrHashTables = L,
    lshParams = LSHParams(
      signatureLength = 2,
      radius = Some(LSH.estimateRadius(ctx)),
      distance = EuclideanDistance,
      seed = seed))

  lazy val ctx = TDAContext(sc, irisDataPointsRDD)

  lazy val exactACC = ExactKNN.exactACC(ctx, ExactKNNParams(k = k, distance = EuclideanDistance))

  it should "pass a smoke test on iris data set" in {
    val fastACC = FastKNN.fastACC(ctx, params)

    val accuracy = KNN.accuracy(fastACC, exactACC)

    accuracy should be >= 0.99
  }

  it should "yield identical results with the same seed value" in {
    val a = FastKNN.fastACC(ctx, params)
    val b = FastKNN.fastACC(ctx, params)

    KNN.accuracy(a, exactACC) shouldBe KNN.accuracy(b, exactACC)
  }

  it should "yield increasing accuracy with increasing L" in {
    val accuracies =
      (1 to 5).map(L => {
        val newParams = params.copy(nrHashTables = L)

        val fastACC = FastKNN.fastACC(ctx, newParams)

        (L, KNN.accuracy(fastACC, exactACC))
      })

    accuracies.sliding(2, 1).foreach{ case Seq(a, b) => a should be <= b}
  }

}