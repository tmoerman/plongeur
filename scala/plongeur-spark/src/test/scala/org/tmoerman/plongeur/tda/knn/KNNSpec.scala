package org.tmoerman.plongeur.tda.TestCommons

import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.Commons._
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.tda.knn.SampledKNN.{SampledKNNParams, apply}
import org.tmoerman.plongeur.tda.knn.{ExactKNN, _}

/**
  * @author Thomas Moerman
  */
class KNNSpec extends FlatSpec with SharedSparkContext with Matchers {

  def makeCtx = TDAContext(sc, sc.parallelize(points))

  val exactKNNParams = ExactKNNParams(2, EuclideanDistance)

  def exact(ctx: TDAContext) = ExactKNN(ctx, exactKNNParams)

  behavior of "accuracy of exact kNN"

  it should "yield 100% with respect to itself" in {
    val ctx = makeCtx

    relativeAccuracy(exact(ctx), exact(ctx)) shouldBe 1.0
  }

  it should "yield 100% with respect to a fixed size sampled kNN" in {
    val ctx = makeCtx

    val sampled = apply(ctx, SampledKNNParams(2, Left(3), EuclideanDistance))

    relativeAccuracy(exact(ctx), sampled) shouldBe 1.0
  }

  it should "yield 100% accuracy with respect to a stochastically sampled kNN" in {
    val ctx = makeCtx

    val sampled = apply(ctx, SampledKNNParams(2, Right(0.33), EuclideanDistance)(seed = 1l))

    relativeAccuracy(exact(ctx), sampled) shouldBe 1.0
  }

  behavior of "symmetric kNN"

  val _a = 0
  val _b = 1
  val _c = 2
  val _d = 3
  val _e = 4

  val asymmetricGraph = Seq(
    (_a, bpq(2) += ((_b, 4d), (_c, 1d))),
    (_b, bpq(2) += ((_a, 4d), (_d, 5d))),
    (_c, bpq(2) += ((_a, 1d), (_d, 1d))),
    (_d, bpq(2) += ((_c, 1d), (_e, 3d))),
    (_e, bpq(2) += ((_c, 3d), (_d, 3d))))

  def asymmetricRDD: KNN_RDD = sc.parallelize(asymmetricGraph)

  it should "correctly compute the mutual weighted symmetric kNN graph (AND)" in {
    val mutualSymmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = true))
        .map{ case (i, bpq) => (i, bpq.toSet) }
        .collectAsMap
        .toMap

    mutualSymmetric shouldBe Map(
      _a -> Set((_b, 4d), (_c, 1d)),
      _b -> Set((_a, 4d)),
      _c -> Set((_a, 1d), (_d, 1d)),
      _d -> Set((_c, 1d), (_e, 3d)),
      _e -> Set((_d, 3d))
    )
  }

  it should "correctly compute the mutual unweighted symmetric kNN graph (AND)" in {
    val mutualSymmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = true, weighted = false))
        .map{ case (i, bpq) => (i, bpq.toSet) }
        .collectAsMap
        .toMap

    mutualSymmetric shouldBe Map(
      _a -> Set((_b, 1d), (_c, 1d)),
      _b -> Set((_a, 1d)),
      _c -> Set((_a, 1d), (_d, 1d)),
      _d -> Set((_c, 1d), (_e, 1d)),
      _e -> Set((_d, 1d))
    )
  }

  it should "correctly compute the symmetric weighted kNN graph (OR)" in {
    val symmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false))
        .map{ case (i, bpq) => (i, bpq.toSet) }
        .collectAsMap
        .toMap

    symmetric shouldBe Map(
      _a -> Set((_b, 4d), (_c, 1d)),
      _b -> Set((_d, 5d), (_a, 4d)),
      _c -> Set((_e, 3d), (_d, 1d), (_a, 1d)),
      _d -> Set((_b, 5d), (_c, 1d), (_e, 3d)),
      _e -> Set((_c, 3d), (_d, 3d))
    )
  }

  it should "correctly compute the symmetric unweighted kNN graph (OR)" in {
    val symmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false, weighted = false))
        .map{ case (i, bpq) => (i, bpq.toSet) }
        .collectAsMap
        .toMap

    symmetric shouldBe Map(
      _a -> Set((_b, 1d), (_c, 1d)),
      _b -> Set((_d, 1d), (_a, 1d)),
      _c -> Set((_e, 1d), (_d, 1d), (_a, 1d)),
      _d -> Set((_b, 1d), (_c, 1d), (_e, 1d)),
      _e -> Set((_c, 1d), (_d, 1d))
    )
  }

  it should "correctly compute the symmetric weighted kNN graph with half non-mutual edge weights (OR)" in {
    val symmetric =
      symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false))
        .map{ case (i, bpq) => (i, bpq.toSet) }
        .collectAsMap
        .toMap

    symmetric shouldBe Map(
      _a -> Set((_b, 4d), (_c, 1d)),
      _b -> Set((_d, 5d), (_a, 4d)),
      _c -> Set((_e, 3d), (_d, 1d), (_a, 1d)),
      _d -> Set((_b, 5d), (_c, 1d), (_e, 3d)),
      _e -> Set((_c, 3d), (_d, 3d))
    )
  }

  it should "convert to a SparseMatrix correctly" in {
    val symmetric = symmetricize(asymmetricRDD, SymmetricizeParams(mutual = false))

    val m = toSparseMatrix(5, symmetric.asInstanceOf[KNN_RDD_Like])

    m.numRows shouldBe 5
  }

}