package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import org.apache.spark.RangePartitioner
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.FastKNN_BAK._

import scala.util.hashing.MurmurHash3
import scalaz.Scalaz._

/**
  * TODO explain M/R strategy.
  *
  * @author Thomas Moerman
  */
object FastKNN {

  /**
    * @param ctx
    * @param kNNParams
    * @return Returns a _directed_ kNN graph data structure.
    */
  def apply(ctx: TDAContext, kNNParams: FastKNNParams): KNN_RDD = {
    import kNNParams._
    import lshParams._

    type HashProjection = Double
    type TableIndex = Int
    type HashProjectionFunction = (DataPoint) => (HashProjection, (TableIndex, DataPoint))

    val random = new JavaRandom(lshParams.seed)

    val N = ctx.N
    val D = ctx.D
    val indexBound = ctx.indexBound
    implicit val d = distance

    /**
      * @param tableIndex
      * @return Returns a new hash projection function, generated with a new seed value.
      */
    def hashProjectionFunction(tableIndex: Index): HashProjectionFunction = {
      val newSeed = random.nextLong // different seed for each hash table

      val w = randomUniformVector(signatureLength, newSeed)

      val hashFunction = LSH.makeHashFunction(D, lshParams.copy(seed = newSeed))

      def hashProjection(p: DataPoint): Distance =
        hashFunction
          .map(_.signature(p.features))
          .map(toVector(signatureLength, _) dot w)
          .get

      (p: DataPoint) => {
        val offset = tableIndex * N

        val tableHash = offset + hashProjection(p)

        (tableHash, (tableIndex, p))
      }
    }

    /**
      * @param hashProjectionFunctions
      * @return Returns the KNN_RDD result, calculated in function of the specified collection of
      *         hash projection functions.
      *
      *         The motivation for this mechanism is to provide a means to break up the complete job
      *         into a number of smaller jobs when running the complete job on hardware with insufficient
      *         memory would result in suffocating the JVM.
      */
    def runJob(hashProjectionFunctions: Seq[HashProjectionFunction]): KNN_RDD = {

      val bc = ctx.sc.broadcast(hashProjectionFunctions)
      val byTableHash =
        ctx
          .dataPoints
          .flatMap(p => bc.value.map(_.apply(p)))

      val rangePartitioner = new RangePartitioner(ctx.sc.defaultParallelism, byTableHash)
      val byBlockId =
        byTableHash
          .repartitionAndSortWithinPartitions(rangePartitioner)
          .mapPartitionsWithIndex{ case (partitionIndex, it) =>
            it.zipWithIndex.map{ case ((_, (tableIdx, p)), idx) => (toBlockId(tableIdx, partitionIndex, idx, blockSize), p) }}

      val knn_RDD =
        byBlockId
          .combineByKey(init(k), concat(k), merge(indexBound))
          .flatMap{ case (_, acc) => acc.map{ case (p, bpq) => (p.index, bpq) }}
          .reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }

      knn_RDD
    }

    val nrHashTablesPerJob = Math.ceil(nrHashTables.toDouble / nrJobs).toInt

    val EMPTY: Map[Index, BPQ] = Map.empty

    val result =
      (1 to nrHashTables)
        .grouped(nrHashTablesPerJob)
        .map(tableIndices => tableIndices.map(hashProjectionFunction))
        .map(runJob)

        .foldLeft(EMPTY){ case (acc, rdd) => {
          val accNext = rdd.collectAsMap.toMap

          if (acc.isEmpty) accNext else (acc intersectWith accNext)(_ ++= _)
        }}

    ctx.sc.parallelize(result.toSeq) // TODO hacketyhack

    // ???
  }

  def toBlockId(tableIndex: Int, partitionIndex: Int, pointIndexInPartition: Int, blockSize: Int): Long = {
    val localBlockIndex = pointIndexInPartition / blockSize

    MurmurHash3.productHash((tableIndex, partitionIndex, localBlockIndex))
  }

}