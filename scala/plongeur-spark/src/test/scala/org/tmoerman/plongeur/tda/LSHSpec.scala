package org.tmoerman.plongeur.tda

import org.apache.spark.mllib.linalg.Vectors._
import org.scalatest.{Matchers, FlatSpec}
import org.tmoerman.plongeur.tda.Distances.CosineDistance
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.{TestResources, SparkContextSpec}

/**
  * @author Thomas Moerman
  */
class LSHSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  behavior of "toVector"

  lazy val ctx = TDAContext(sc, irisDataPointsRDD)

  it should "Vectors of size signatureLength for CosineDistance" in {
    val params = LSHParams(5, None, CosineDistance, 666L)
    import params._

    val hashFunction = LSH.makeHashFunction(ctx.D, params).get

    val hashValues =
      ctx
        .dataPoints
        .map(p => LSH.toVector(signatureLength, hashFunction.signature(p.features)))
        .collect

    hashValues.map(_.length).toSet shouldBe Set(signatureLength)
  }

}