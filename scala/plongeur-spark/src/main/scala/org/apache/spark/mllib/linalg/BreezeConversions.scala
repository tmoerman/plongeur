package org.apache.spark.mllib.linalg

import breeze.linalg.{Matrix => BreezeMatrix, Vector => BreezeVector}
import org.apache.spark.mllib.linalg.{Vector => MLLibVector, Matrix => MLLibMatrix}

/**
  * Hackety-hack conversions between Spark MLLib and Breeze vectors and matrices.
  *
  * @author Thomas Moerman
  */
object BreezeConversions extends Serializable {

  implicit class MLLibVectorConversion(val vector: MLLibVector) extends AnyVal {
    def toBreeze: BreezeVector[Double] = vector.toBreeze
  }

  implicit class BreezeVectorConversion(val vector: BreezeVector[Double]) extends AnyVal {
    def toMLLib: MLLibVector = Vectors.fromBreeze(vector)
  }

  implicit class MLLibMatrixConversion(val matrix: MLLibMatrix) extends AnyVal {
    def toBreeze: BreezeMatrix[Double] = matrix.toBreeze
  }

  implicit class BreezeMatrixConversion(val matrix: BreezeMatrix[Double]) extends AnyVal {
    def toMLLib: MLLibMatrix = Matrices.fromBreeze(matrix)
  }

}