package org.apache.spark.mllib.linalg

import org.apache.spark.mllib.linalg.{Vector => MLLibVector}
import breeze.linalg.{Vector => BreezeVector}

/**
  * Hackety-hack conversions between Spark MLLib and Breeze vectors.
  *
  * @author Thomas Moerman
  */
object VectorConversions extends Serializable {

  implicit class MLLibVectorConversion(val vector: MLLibVector) extends AnyVal {

    def toBreeze: BreezeVector[Double] = vector.asBreeze

  }

  implicit class BreezeVectorConversion(val vector: BreezeVector[Double]) extends AnyVal {

    def toMLLib: MLLibVector = Vectors.fromBreeze(vector)

  }

}