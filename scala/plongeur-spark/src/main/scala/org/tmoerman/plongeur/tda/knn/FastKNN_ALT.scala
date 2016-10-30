package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import org.apache.spark.Partitioner
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH._
import org.tmoerman.plongeur.tda.Model._

/**
  * Alternative implementation that partitions by table index.
  * Easier to verify correctness but might potentially not use the computation resources efficiently
  * because we only use a number of partitions in function of the number of hash tables.
  *
  * @author Thomas Moerman
  */
object FastKNN_ALT {

  def apply(ctx: TDAContext, kNNParams: FastKNNParams): KNN_RDD = {
    import kNNParams._
    import lshParams._

    val bc = ctx.sc.broadcast(hashProjectionFunctions(ctx, kNNParams, seed))

    val N = ctx.N
    val indexBound = ctx.indexBound
    implicit val d = distance

    val byTableHash =
      ctx
        .dataPoints
        .flatMap(p => bc.value.map(_.apply(p)))
    
    byTableHash
      .repartitionAndSortWithinPartitions(LeftPartitioner[Int](ctx.sc.defaultParallelism))
      .mapPartitionsWithIndex{case (partitionIdx, it) =>
        it.zipWithIndex.map{ case (((_, projection), p), idx) => (toBlockIndex(N, partitionIdx, idx, blockSize), p) }}
      .combineByKey(init(k), concat(k), merge(indexBound))
      .flatMap{ case (_, acc) => acc.map{ case (p, bpq) => (p.index, bpq) }}
      .reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }
  }

  case class LeftPartitioner[K1](nrPartitions: Int) extends Partitioner {

    override def numPartitions: Int = nrPartitions

    override def getPartition(key: Any): Int =
      key.asInstanceOf[(K1, _)]._1.hashCode() % numPartitions

  }

  /**
    * RULES:
    * - table index == partition index
    * - element index in partition is the sort order used to compute the local blockId
    *
    * @param N
    * @param partitionIndex
    * @param pointIndexInPartition
    * @param blockSize
    * @return Returns the block index.
    */
  def toBlockIndex(N: Int,
                   partitionIndex: Int,
                   pointIndexInPartition: Int,
                   blockSize: Int) = {

    val localBlockId = pointIndexInPartition / blockSize

    val partitionOffset = partitionIndex * N

    val globalBlockId = partitionOffset + localBlockId

    globalBlockId
  }

  /**
    * @param ctx
    * @param kNNParams
    * @param seed
    * @return Returns a Seq of functions f: DataPoint -> (tableHash, (table, DataPoint))
    */
  def hashProjectionFunctions(ctx: TDAContext, kNNParams: FastKNNParams, seed: Long) = {
    import kNNParams._
    val D = ctx.D

    val random = new JavaRandom(lshParams.seed)

    (1 to nrHashTables)
      .map(_ - 1)
      .map(tableIndex => {
        val newSeed = random.nextLong // different seed for each hash table
        val params = kNNParams.copy(lshParams = lshParams.copy(seed = newSeed))

        import params.lshParams
        import params.lshParams._

        lazy val w = FastKNN_BAK.randomUniformVector(signatureLength, newSeed)

        val hashFunction = LSH.makeHashFunction(D, lshParams)

        def hashProjection(p: DataPoint): Distance =
          hashFunction
            .map(_.signature(p.features))
            .map(toVector(signatureLength, _) dot w)
            .get

        (p: DataPoint) => ((tableIndex, hashProjection(p)), p)
      })
  }

}