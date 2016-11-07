package org.tmoerman.plongeur.tda.TestCommons

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.Commons._
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.tda.knn.SampledKNN.{SampledKNNParams, apply}
import org.tmoerman.plongeur.tda.knn.{ExactKNN, _}
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
    relativeAccuracy(exact, exact) shouldBe 1.0
  }

  it should "yield 100% with respect to a fixed size sampled kNN" in {
    val sampled = apply(ctx, SampledKNNParams(2, Left(3), EuclideanDistance))

    relativeAccuracy(exact, sampled) shouldBe 1.0
  }

  it should "yield 100% accuracy with respect to a stochastically sampled kNN" in {
    val sampled = apply(ctx, SampledKNNParams(2, Right(0.33), EuclideanDistance)(seed = 1l))

    relativeAccuracy(exact, sampled) shouldBe 1.0
  }

  behavior of "symmetric kNN"

  val _a = 1
  val _b = 2
  val _c = 3
  val _d = 4
  val _e = 5

  val asymmetricGraph = Seq(
    (_a, bpq(2) += ((_b, 4.), (_c, 1.))),
    (_b, bpq(2) += ((_a, 4.), (_d, 5.))),
    (_c, bpq(2) += ((_a, 1.), (_d, 1.))),
    (_d, bpq(2) += ((_c, 1.), (_e, 3.))),
    (_e, bpq(2) += ((_c, 3.), (_d, 3.))))

  val asymmetricRDD: KNN_RDD = sc.parallelize(asymmetricGraph)

  it should "correctly compute the mutual weighted symmetric kNN graph (AND)" in {
    val mutualSymmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = true))
        .collectAsMap
        .toMap

    mutualSymmetric shouldBe Map(
      _a -> Set((_b, 4.), (_c, 1.)),
      _b -> Set((_a, 4.)),
      _c -> Set((_a, 1.), (_d, 1.)),
      _d -> Set((_c, 1.), (_e, 3.)),
      _e -> Set((_d, 3.))
    )
  }

  it should "correctly compute the mutual unweighted symmetric kNN graph (AND)" in {
    val mutualSymmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = true, weighted = false))
        .collectAsMap
        .toMap

    mutualSymmetric shouldBe Map(
      _a -> Set((_b, 1.), (_c, 1.)),
      _b -> Set((_a, 1.)),
      _c -> Set((_a, 1.), (_d, 1.)),
      _d -> Set((_c, 1.), (_e, 1.)),
      _e -> Set((_d, 1.))
    )
  }

  it should "correctly compute the symmetric weighted kNN graph (OR)" in {
    val symmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false))
        .collectAsMap
        .toMap

    symmetric shouldBe Map(
      _a -> Set((_b, 4.), (_c, 1.)),
      _b -> Set((_d, 5.), (_a, 4.)),
      _c -> Set((_e, 3.), (_d, 1.), (_a, 1.)),
      _d -> Set((_b, 5.), (_c, 1.), (_e, 3.)),
      _e -> Set((_c, 3.), (_d, 3.))
    )
  }

  it should "correctly compute the symmetric unweighted kNN graph (OR)" in {
    val symmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false, weighted = false))
        .collectAsMap
        .toMap

    symmetric shouldBe Map(
      _a -> Set((_b, 1.), (_c, 1.)),
      _b -> Set((_d, 1.), (_a, 1.)),
      _c -> Set((_e, 1.), (_d, 1.), (_a, 1.)),
      _d -> Set((_b, 1.), (_c, 1.), (_e, 1.)),
      _e -> Set((_c, 1.), (_d, 1.))
    )
  }

  it should "correctly compute the symmetric weighted kNN graph with half non-mutual edge weights (OR)" in {
    val symmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false))
        .collectAsMap
        .toMap

    symmetric shouldBe Map(
      _a -> Set((_b, 4.), (_c, 1.)),
      _b -> Set((_d, 5.), (_a, 4.)),
      _c -> Set((_e, 3.), (_d, 1.), (_a, 1.)),
      _d -> Set((_b, 5.), (_c, 1.), (_e, 3.)),
      _e -> Set((_c, 3.), (_d, 3.))
    )
  }

}