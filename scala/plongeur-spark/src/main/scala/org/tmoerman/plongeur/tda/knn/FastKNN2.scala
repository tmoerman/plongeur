package org.tmoerman.plongeur.tda.knn

import java.util.{Random => JavaRandom}

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distances._
import org.tmoerman.plongeur.tda.LSH
import org.tmoerman.plongeur.tda.LSH._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.knn.FastKNN._
import org.tmoerman.plongeur.tda.knn.KNN._
import org.tmoerman.plongeur.util.IterableFunctions._

/**
  * A second attempt at implementing FastKNN, this time leveraging Spark's `repartitionAndSortWithinPartitions`.
  *
  * @author Thomas Moerman
  */
object FastKNN2 {

  class MyPartitioner(partitions: Int) extends Partitioner {

    override def numPartitions: Int = partitions

    override def getPartition(key: Any): Int =
      key.asInstanceOf[(Int, Double)]._1.hashCode() % numPartitions

  }

  def fastACC(ctx: TDAContext, kNNParams: FastKNNParams): ACC = {
    import kNNParams._
    import lshParams._

    val random = new JavaRandom(lshParams.seed)

    type T = (DataPoint, BPQ)
    type IT = RDD[(Index, T)]

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

          (p: DataPoint) => ((table, hashProjection(p)), p)
        })

    val bc = ctx.sc.broadcast(hashProjections)

    implicit val d = distance
    implicit val k = kNNParams.k

    def toBlockId(table: Int, globalIndex: Long): Long = (globalIndex - table*N) / blockSize

    ctx
      .dataPoints
      .flatMap(p => bc.value.map(_.apply(p)))
      .zipWithIndex
      .map{ case (((table, projection), p), globalIndex) => ((table, toBlockId(table, globalIndex)), p) }
      .combineByKey(init, concat, merge)
      .flatMap(_._2)
      .collect
      .toList
  }

}