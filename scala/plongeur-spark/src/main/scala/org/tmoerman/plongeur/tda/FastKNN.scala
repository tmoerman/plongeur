package org.tmoerman.plongeur.tda

import breeze.linalg.{DenseVector => BDV}
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH.{LSHParams, toVector}
import org.tmoerman.plongeur.tda.Model._
import breeze.linalg.{CSCMatrix => BSM}
import org.tmoerman.plongeur.util.BoundedPriorityQueue

/**
  * See Fast kNN Graph Construction with Locality Sensitive Hashing
  *   -- Yan-Ming Zhang, Kaizhu Huang, Guanggang Geng, and Cheng-Lin Liu
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
  case class KNNParams(k: Int,
                       blockSize: Int,
                       nrHashTables: Int = 1,
                       lshParams: LSHParams)

  /**
    * @param ctx
    * @param kNNParams
    * @return Returns a kNN sparse matrix.
    */
  def apply(ctx: TDAContext, kNNParams: KNNParams): BSM[Distance] = {
    import kNNParams._

    val combined =
      (1 to nrHashTables)
        .par
        .map(_ => basic(ctx, kNNParams))
        .reduce(combine)

    // TODO neighbour propagation?

    toSparseMatrix(ctx.N, combined)
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

        (p, bpq1 ++= bpq2)
      }}

  /**
    * @return Returns a Breeze sparse matrix (BSM) in function of the calculated kNN data structure.
    */
  def toSparseMatrix(N: Int, acc: ACC): BSM[Distance] = {
    val kNN = acc.map{ case (_, bpq) => bpq.toSeq.sortBy(_._1) }

    val distances   = kNN.flatMap(_.map(_._2)).toArray
    val rowIndices  = kNN.map(_.size).toArray
    val colPointers = kNN.flatMap(_.map(_._1)).toArray

    new BSM[Distance](distances, N, N, colPointers, rowIndices)
  }

  def basic(ctx: TDAContext, kNNParams: KNNParams): ACC = {
    import kNNParams._
    import lshParams._

    val hashFunction = LSH.makeHashFunction(ctx.D, lshParams)

    val w: BDV[Distance] = BDV.rand(signatureLength)

    def linearHashProjection(p: DataPoint): Distance =
      hashFunction
        .map(_.signature(p.features))
        .map(toVector(_) dot w)
        .get // TODO propagate failure correctly

    def toBlockId(orderPosition: Long) = orderPosition / blockSize

    implicit val N = ctx.N
    implicit val d = distance

    ctx
      .dataPoints
      .map(p => (linearHashProjection(p), p))       // key by hash projection
      .sortByKey()
      .values
      .zipWithIndex
      .map { case (p, idx) => (toBlockId(idx), p) } // key by block ID
      .combineByKey(init, concat, union)            // bipartite merge within block ID
      .values
      .treeReduce(union)                            // bipartite merge across block IDs
      .sortBy(_._1.index)
  }

  type PQEntry = (Index, Distance)
  type BPQ = BoundedPriorityQueue[PQEntry]
  type ACC = List[(DataPoint, BPQ)]
  implicit val ORD = Ordering.by[PQEntry, Distance](_._2)

  /**
    * @return Returns a new accumulator.
    */
  def init(p: DataPoint)(implicit k: Int): ACC = (p, new BPQ(k)) :: Nil

  /**
    * @return Returns an updated accumulator.
    */
  def concat(acc: ACC, p: DataPoint)(implicit k: Int, distance: DistanceFunction): ACC = {
    val distancesByIndex = acc.map{ case (q, pq) => (q.index, distance(p, q)) }

    (p, new BPQ(k) ++= distancesByIndex) ::
    (acc, distancesByIndex)
      .zipped
      .map{ case ((q, pq), entry) => (q, pq += entry) }
  }

  /**
    * @return Returns a merged accumulator,
    *         cfr. G = U{g_1}, cfr. basic_ann_by_lsh(X, k, block-sz), p666 Y.-M. Zhang et al.
    */
  def union(acc1: ACC, acc2: ACC)(implicit distance: DistanceFunction): ACC = for {
    (p1, bpq1) <- acc1
    (p2, bpq2) <- acc2

    p_bpq <- {
      val d = distance(p1, p2)

      (p1, bpq1 += ((p2.index, d))) :: (p2, bpq2 += ((p1.index, d))) :: Nil
    }
  } yield p_bpq

}