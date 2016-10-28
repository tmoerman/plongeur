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
object FastKNN extends Serializable {

  /**
    * @param k The k in kNN.
    * @param nrHashTables Also known as the L parameter, cfr. LSH literature.
    * @param blockSize
    * @param lshParams
    */
  case class FastKNNParams(k: Int,
                           blockSize: Int,
                           nrHashTables: Int = 1,
                           lshParams: LSHParams) {
    require(k > 0)
    require(nrHashTables > 0)
  }

  /**
    * @param ctx
    * @param kNNParams
    * @return
    */
  def apply(ctx: TDAContext, kNNParams: FastKNNParams): kNN_RDD = {
    import kNNParams._

    val random = new JavaRandom(lshParams.seed)

    def combine1(a: kNN_RDD, b: kNN_RDD) = (a join b).mapValues[BPQ]{ case (bpq1, bpq2) => bpq1 ++= bpq2 }

    def combine2(a: kNN_RDD, b: kNN_RDD) = (a ++ b).reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }

    (1 to nrHashTables)
      .map(_ => {
        val newSeed = random.nextLong // different seed for each hash table
        val params = kNNParams.copy(lshParams = lshParams.copy(seed = newSeed))

        basic(ctx, params) })
      .reduce(combine1)
  }

  /**
    * @param ctx
    * @param kNNParams
    * @return
    */
  def basic(ctx: TDAContext, kNNParams: FastKNNParams): kNN_RDD = {
    import ctx.indexBound
    import kNNParams._
    import lshParams._

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

  /**
    * @return Returns a new accumulator.
    */
  def init(k: Int)(p: DataPoint): Accumulator = (p, bpq(k)) :: Nil

  /**
    * @return Returns an updated accumulator.
    */
  def concat(k: Int)(acc: Accumulator, p: DataPoint)(implicit distance: DistanceFunction): Accumulator = {
    val distances = acc.map{ case (q, bpq) => (q.index, distance(p, q)) }

    val newEntry = (p, distances.foldLeft(bpq(k)){ case (bpq, pair) => bpq += pair })

    val updated =
      (acc, distances)
        .zipped
        .map{ case ((q, bpq), (_, d)) => (q, bpq += ((p.index, d))) }

    newEntry :: updated
  }

  /**
    * Assumes accumulators a and b's data points are mutually exclusive.
    *
    * @return Returns a merged accumulator,
    *         cfr. G = U{g_1}, cfr. basic_ann_by_lsh(X, k, block-sz), p666 Y.-M. Zhang et al.
    */
  def merge(N: Int)(a: Accumulator, b: Accumulator)(implicit distance: DistanceFunction): Accumulator = {
    implicit val ORD = Ordering.by((d: DataPoint) => d.index)

    val distances =
      (a.map(_._1) cartesian b.map(_._1))
        .flatMap{ case (p, q) => {
          val d = distance(p, q)

          (p.index, q.index, d) :: (q.index, p.index, d) :: Nil
        }}

    val cache = SparseMatrix.fromCOO(N, N, distances)

    def merge(acc: Accumulator, arg: Accumulator): Accumulator =
      acc.map{ case (p, bpq) => (p, bpq ++= arg.map{ case (q, _) => (q.index, cache(p.index, q.index)) })}

    merge(a, b) ::: merge(b, a)
  }

}