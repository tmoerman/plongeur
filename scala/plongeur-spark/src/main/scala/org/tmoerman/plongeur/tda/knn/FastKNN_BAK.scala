package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import breeze.linalg.{DenseVector => BDV}
import org.apache.spark.mllib.linalg.SparseMatrix
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH.{LSHParams, toVector}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.util.IterableFunctions._


/**
  * See "Fast kNN Graph Construction with Locality Sensitive Hashing"
  *   -- Yan-Ming Zhang, Kaizhu Huang, Guanggang Geng, and Cheng-Lin Liu
  *
  * Implementation powered by Spark.
  *
  * @author Thomas Moerman
  */
object FastKNN_BAK extends Serializable {

  /**
    * @param ctx
    * @param kNNParams
    * @return
    */
  def apply(ctx: TDAContext, kNNParams: FastKNNParams): KNN_RDD = {
    import kNNParams._

    val random = new JavaRandom(lshParams.seed)

    // def combine2(a: KNN_RDD, b: KNN_RDD) = (a ++ b).reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }

    (1 to nrHashTables)
      .map(_ => {
        val newSeed = random.nextLong // different seed for each hash table
        val params = kNNParams.copy(lshParams = lshParams.copy(seed = newSeed))

        basic(ctx, params) })
      .reduce(combine)
  }

  /**
    * @param ctx
    * @param kNNParams
    * @return
    */
  def basic(ctx: TDAContext, kNNParams: FastKNNParams): KNN_RDD = {
    import kNNParams._
    import lshParams._

    val indexBound = ctx.indexBound

    val hashFunction = LSH.makeHashFunction(ctx.D, lshParams)

    lazy val w = randomUniformVector(signatureLength, seed)

    def hashProjection(p: DataPoint): Distance =
      hashFunction
        .map(_.signature(p.features))
        .map(toVector(signatureLength, _) dot w)
        .get // TODO propagate failure correctly

    def toBlockId(orderPosition: Long) = orderPosition / blockSize

    implicit val d = distance

    ctx
      .dataPoints
      .map(p => (hashProjection(p), p)) // key by hash projection
      .sortByKey()
      .values
      .zipWithIndex
      .map { case (p, idx) => (toBlockId(idx), p) } // key by block ID
      .combineByKey(init(k), concat(k), merge(indexBound)) // bipartite merge within block ID
      .flatMap{ case (_, acc) => acc.map{ case (p, bpq) => (p.index, bpq) }}
  }

  def randomUniformVector(length: Int, seed: Long): BDV[Distance] = {
    val random = new JavaRandom(seed)

    BDV.apply((1 to length).map(_ => random.nextDouble): _*)
  }

}