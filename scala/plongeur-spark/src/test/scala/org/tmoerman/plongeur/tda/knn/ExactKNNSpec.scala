package org.tmoerman.plongeur.tda.knn

import org.tmoerman.plongeur.tda.Distances.EuclideanDistance
import org.tmoerman.plongeur.tda.Model.TDAContext
import org.tmoerman.plongeur.tda.knn.ExactKNN.ExactKNNParams
import org.tmoerman.plongeur.test.SparkContextSpec

/**
  * @author Thomas Moerman
  */
class ExactKNNSpec extends KNNSpec with SparkContextSpec {

  "ExactKNN" should "yield correct frequencies" in {
    val kNNParams = ExactKNNParams(k = 2, distance = EuclideanDistance)

    val rdd = sc.parallelize(points)
    val ctx = TDAContext(sc, rdd)

    val acc = ExactKNN.toACC(ctx, kNNParams)

    assertDistanceFrequencies(acc)
  }

}