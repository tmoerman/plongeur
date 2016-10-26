package org.tmoerman.plongeur.tda.knn

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.ExactKNN._
import org.tmoerman.plongeur.tda.knn.SampledKNN.{apply, SampledKNNParams}
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class KNNSpec extends FlatSpec with SparkContextSpec with Matchers {

  behavior of "accuracy of exact kNN"

  lazy val rdd = sc.parallelize(points)
  lazy val ctx = TDAContext(sc, rdd)

  val exactKNNParams = ExactKNNParams(2, EuclideanDistance)

  val exact = ExactKNN(ctx, exactKNNParams)

  it should "yield 100% with respect to itself" in {
    KNN.accuracy(exact, exact) shouldBe 1.0
  }

  it should "yield 100% with respect to a fixed size sampled kNN" in {
    val sampled = apply(ctx, SampledKNNParams(2, Left(3), EuclideanDistance))

    KNN.accuracy(exact, sampled) shouldBe 1.0
  }

  it should "yield 100% accuracy with respect to a stochastically sampled kNN" in {
    val sampled = apply(ctx, SampledKNNParams(2, Right(0.33), EuclideanDistance)(seed = 1l))

    KNN.accuracy(exact, sampled) shouldBe 1.0
  }

}