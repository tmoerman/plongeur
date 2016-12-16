package org.tmoerman.plongeur.tda

import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Distances.CosineDistance
import org.tmoerman.plongeur.tda.LSH.LSHParams
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.test.TestResources

/**
  * @author Thomas Moerman
  */
class LSHSpec extends FlatSpec with SharedSparkContext with TestResources with Matchers {

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