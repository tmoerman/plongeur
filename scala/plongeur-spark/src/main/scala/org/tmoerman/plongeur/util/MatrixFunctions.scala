package org.tmoerman.plongeur.util

import org.apache.spark.mllib.linalg.{SparseVector, SparseMatrix}
import scala.collection.mutable.{ArrayBuilder => MArrayBuilder}

/**
  * @author Thomas Moerman
  */
object MatrixFunctions {

  implicit def pimpSparse(m: SparseMatrix): SparseMatrixFunctions = new SparseMatrixFunctions(m)

}

class SparseMatrixFunctions(m: SparseMatrix) {

  def rowVectors = rowColIter(m.transpose)

  def colVectors = rowColIter(m)

  /**
    * See MLLib 2.0
    * https://github.com/apache/spark/blob/master/mllib/src/main/scala/org/apache/spark/mllib/linalg/Matrices.scala
    */
  @deprecated("remove after upgrade to Spark 2.0")
  private def rowColIter(m: SparseMatrix): Iterator[SparseVector] = {
    if (m.isTransposed) {
      val indicesArray = Array.fill(m.numCols)(MArrayBuilder.make[Int])
      val valuesArray = Array.fill(m.numCols)(MArrayBuilder.make[Double])
      var i = 0
      while (i < m.numRows) {
        var k = m.colPtrs(i)
        val rowEnd = m.colPtrs(i + 1)
        while (k < rowEnd) {
          val j = m.rowIndices(k)
          indicesArray(j) += i
          valuesArray(j) += m.values(k)
          k += 1
        }
        i += 1
      }
      Iterator.tabulate(m.numCols) { j =>
        val ii = indicesArray(j).result()
        val vv = valuesArray(j).result()
        new SparseVector(m.numRows, ii, vv)
      }
    } else {
      Iterator.tabulate(m.numCols) { j =>
        val colStart = m.colPtrs(j)
        val colEnd = m.colPtrs(j + 1)
        val ii = m.rowIndices.slice(colStart, colEnd)
        val vv = m.values.slice(colStart, colEnd)
        new SparseVector(m.numRows, ii, vv)
      }
    }
  }

}