package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import org.apache.spark.Partitioner
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.FastKNN._
import org.tmoerman.plongeur.tda.knn.KNN._

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
            val key = table * N + hashProjection(p)

            (key, (table, p))
          }
        })

    val bc = ctx.sc.broadcast(hashProjections)

    implicit val d = distance
    implicit val k = kNNParams.k

    def toBlockId(table: Int, globalIndex: Long): Long = {
      val offSet = table * N

      offSet + (globalIndex - offSet) / blockSize
    }


    class MyPartitioner(partitions: Int) extends Partitioner {

      override def numPartitions: Int = partitions

      override def getPartition(key: Any): Int =
        key.asInstanceOf[(Int, Distance)]._1.hashCode() % numPartitions

    }

    ctx
      .dataPoints
      .flatMap(p => bc.value.map(_.apply(p)))
      .sortByKey()
      .zipWithIndex
      .map{ case ((_, (table, p)), globalIndex) => (toBlockId(table, globalIndex), p) }
      .combineByKey(init, concat, merge)
      .flatMap{ case (_, acc) => acc.map{ case (p, bpq) => (p.index, bpq) }}
      .reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }

//    ctx
//      .dataPoints
//      .flatMap(p => bc.value.map(_.apply(p)))
//      .repartitionAndSortWithinPartitions(new MyPartitioner(ctx.sc.defaultParallelism))
//      .mapPartitionsWithIndex({ case (partIdx, it) => it.zipWithIndex.map{ case (((table, _), p), itIdx) => (toBlockId(table, partIdx*itIdx), p) }}, preservesPartitioning = true)
//      //      .zipWithIndex
//      //      .map{ case (((table, projection), p), globalIndex) => (toBlockId(table, globalIndex), p) }
//      .combineByKey(init, concat, merge)
//      .mapPartitions(_.flatMap(_._2.map{ case (p, bpq) => (p.index, bpq) }), preservesPartitioning = true)
//      //.flatMap{ case (_, acc) => acc.map{ case (p, bpq) => (p.index, bpq) }}
//      .reduceByKey{ case (bpq1, bpq2) => bpq1 ++= bpq2 }
  }

}