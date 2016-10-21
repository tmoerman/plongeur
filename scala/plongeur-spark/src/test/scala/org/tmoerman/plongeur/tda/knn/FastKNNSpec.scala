package org.tmoerman.plongeur.tda.knn

import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distances.{DistanceFunction, EuclideanDistance}
import org.tmoerman.plongeur.tda.Model.{TDAContext, DataPoint}
import org.tmoerman.plongeur.tda.knn.FastKNN._
import org.tmoerman.plongeur.tda.knn.KNN._
import org.tmoerman.plongeur.test.{TestResources, SparkContextSpec}
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

  it should "pass a smoke test on iris data set" in {
    val k = 10

    val blockSize = 30

    //val params = new FastKNNParams(k, c)


  }

}