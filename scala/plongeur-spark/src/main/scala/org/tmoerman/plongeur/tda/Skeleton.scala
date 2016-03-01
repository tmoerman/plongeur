package org.tmoerman.plongeur.tda

import org.apache.spark.Partitioner.defaultPartitioner
import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Distance.{euclidean, DistanceFunction}

import org.tmoerman.plongeur.util.IterableFunctions._
import Covering._
import Clustering._
import Model._

/**
  * @author Thomas Moerman
  */
object Skeleton extends Serializable {

  /**
    * @param lens
    * @param data
    * @param boundaries
    * @param distanceFunction
    * @return
    */
  def execute(lens: Lens,
              data: RDD[DataPoint],
              boundaries: Option[Array[(Double, Double)]] = None,
              distanceFunction: DistanceFunction = euclidean): TDAResult = {

    val bounds = boundaries.getOrElse(toBoundaries(lens.functions, data))

    val levelSetInverse = toLevelSetInverseFunction(lens, bounds)

    val clustersRDD: RDD[Cluster[Any]] =
      data
        .flatMap(dataPoint => levelSetInverse(dataPoint).map(levelSetID => (levelSetID, dataPoint)))
        .repartitionAndSortWithinPartitions(defaultPartitioner(data)) // TODO which partitioner?
        .mapPartitions(_
          .view                                                       // TODO view ok here?
          .groupRepeats(selector = { case (levelSetID, _) => levelSetID })  // group by levelSet // TODO a more efficient streaming version with selector and extractor
          .map(pairs => { val levelSetID = pairs.head._1
                          val dataPoints = pairs.map(_._2)
                          cluster(dataPoints, levelSetID, distanceFunction) }))   // cluster points
        .flatMap(_.map(c => (c.points, c)))   //
        .reduceByKey((c1, c2) => c1)          // collapse equivalent clusters
        .values
        .cache

    val edgesRDD: RDD[Set[Any]] =
      clustersRDD
        .flatMap(cluster => cluster.points.map(p => (p.index, cluster.id))) // melt all clusters by points
        .combineByKey((clusterId: Any) => Set(clusterId),  // TODO turn into groupByKey?
                      (acc: Set[Any], id: Any) => acc + id,
                      (acc1: Set[Any], acc2: Set[Any]) => acc1 ++ acc2)     // create proto-clusters, collapse doubles
        .values
        .flatMap(_.subsets(2))
        .distinct
        .cache

    TDAResult(bounds, clustersRDD, edgesRDD)
  }

  case class TDAResult(val bounds: Array[(Double, Double)],
                       val clustersRDD: RDD[Cluster[Any]],
                       val edgesRDD: RDD[Set[Any]]) extends Serializable {

    lazy val clusters = clustersRDD.collect

    lazy val edges = edgesRDD.collect

  }


}
