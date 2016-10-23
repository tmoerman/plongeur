package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import breeze.linalg.{DenseVector => BDV}
import org.apache.spark.mllib.linalg.SparseMatrix
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH.{LSHParams, toVector}
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.KNN._

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
                           lshParams: LSHParams)

  /**
    * @param ctx
    * @param kNNParams
    * @return Returns a kNN sparse matrix.
    */
  def apply(ctx: TDAContext, kNNParams: FastKNNParams): SparseMatrix = {
    val combined = fastACC(ctx, kNNParams)

    toSparseMatrix(ctx.N, combined)
  }

  /**
    * @param ctx
    * @param kNNParams
    * @return
    */
  def fastACC(ctx: TDAContext, kNNParams: FastKNNParams): ACC = {
    import kNNParams._

    val random = new JavaRandom(lshParams.seed)

    val combined =
      (1 to nrHashTables)
        //.par
        .map(_ => kNNParams.copy(lshParams = lshParams.copy(seed = random.nextLong))) // different seed for each hash table
        .map(params => singleACC(ctx, params))
        .reduce(combine)

    combined
  }

  /**
    * Assumption: acc1 and acc2 both are sorted and have equal size
    *
    * @return Returns G = combine{G_1, G_2, ..., G_l}
    */
  def combine(acc1: ACC, acc2: ACC): ACC =
    (acc1, acc2)
      .zipped
      .map{ case ((p, bpq1), (q, bpq2)) => {
        assert(p.index == q.index) // TODO remove after simplification

        val result = (p, bpq1 ++= bpq2)

        result
      }}

  /**
    * @param ctx
    * @param kNNParams
    * @return TODO
    */
  def singleACC(ctx: TDAContext, kNNParams: FastKNNParams): ACC = {
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
    implicit val k = kNNParams.k

    ctx
      .dataPoints
      .map(p => (hashProjection(p), p))       // key by hash projection
      .sortByKey()
      .values
      .zipWithIndex
      .map { case (p, idx) => (toBlockId(idx), p) } // key by block ID
      .combineByKey(init, concat, union)            // bipartite merge within block ID
      .values
      .treeReduce(union)                            // bipartite merge across block IDs
      .sortBy(_._1.index)
  }

  def randomUniformVector(length: Int, seed: Long): BDV[Distance] = {
    val random = new JavaRandom(seed)

    BDV.apply((1 to length).map(_ => random.nextDouble): _*)
  }

  /**
    * @return Returns a new accumulator.
    */
  def init(p: DataPoint)(implicit k: Int): ACC = (p, bpq(k)) :: Nil

  /**
    * @return Returns an updated accumulator.
    */
  def concat(acc: ACC, p: DataPoint)(implicit k: Int, distance: DistanceFunction): ACC = {
    val distances = acc.map(_._1).map(q => ((p.index, q.index), distance(p, q)))

    (p, bpq(k) ++= distances.map{ case ((p, q), d) => (q, d) }) ::
    (acc, distances.map{ case ((p, q), d) => (p, d) })
      .zipped
      .map{ case ((q, pq), entry) => (q, pq += entry) }
  }

  /**
    * Assumes accumulators a and b's data points are mutually exclusive.
    *
    * @return Returns a merged accumulator,
    *         cfr. G = U{g_1}, cfr. basic_ann_by_lsh(X, k, block-sz), p666 Y.-M. Zhang et al.
    */
  def union(a: ACC, b: ACC)(implicit distance: DistanceFunction): ACC = {
    assert((a.map(_._1).toSet intersect b.map(_._1).toSet).isEmpty) // TODO remove after simplification

    def merge(base: ACC, arg: ACC) =
      base.map{ case (p, bpq) => (p, bpq ++= arg.map{ case (q, _) => (q.index, distance(p, q)) }) }

    merge(a, b) ::: merge(b, a)
  }

}