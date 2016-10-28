package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import org.apache.spark.RangePartitioner
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.FastKNN._

/**
  * TODO explain M/R strategy.
  *
  * @author Thomas Moerman
  */
object FastKNN2 {

  def apply(ctx: TDAContext, kNNParams: FastKNNParams): kNN_RDD = {
    import kNNParams._
    import lshParams._

    val random = new JavaRandom(lshParams.seed)

    val N = ctx.N
    val D = ctx.D
    val indexBound = ctx.indexBound

    val hashProjections =
      (1 to nrHashTables)
        .map(table => {
          val newSeed = random.nextLong // different seed for each hash table
          val params = kNNParams.copy(lshParams = lshParams.copy(seed = newSeed))

          import params.lshParams
          import params.lshParams._

          lazy val w = FastKNN.randomUniformVector(signatureLength, newSeed)

          val hashFunction = LSH.makeHashFunction(D, lshParams)

          def hashProjection(p: DataPoint): Distance =
            hashFunction
              .map(_.signature(p.features))
              .map(toVector(signatureLength, _) dot w)
              .get

          (p: DataPoint) => {
            val tableHash = table * N + hashProjection(p)

            (tableHash, (table, p))
          }
        })

    val bc = ctx.sc.broadcast(hashProjections)

    implicit val d = distance

    val byTableHash =
      ctx
        .dataPoints
        .flatMap(p => bc.value.map(_.apply(p)))

    val partitioner = new RangePartitioner(ctx.sc.defaultParallelism, byTableHash)

    def toBlockId(table: Int, globalIndex: Long): Long = {
      val offSet = table * N
      offSet + (globalIndex - offSet) / blockSize
    }

    byTableHash
      .repartitionAndSortWithinPartitions(partitioner)
      .mapPartitionsWithIndex(
        { case (partIdx, it) => it.zipWithIndex.map{ case ((_, (table, p)), idx) => (toBlockId(table, partIdx * N + idx), p) }},
        preservesPartitioning = true)
      .combineByKey(init(k), concat(k), merge(indexBound))
      .flatMap{ case (_, acc) => acc.map{ case (p, bpq) => (p.index, bpq) }}
      .reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }
  }

}