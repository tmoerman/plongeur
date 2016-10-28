package org.tmoerman.plongeur.tda

import java.util.UUID

import org.apache.spark.rdd.RDD
import org.tmoerman.plongeur.tda.Model._
import shapeless.{HList, HNil}

/**
  * @author Thomas Moerman
  */
object TDA_LSH extends TDA {

  def apply(ctx: TDAContext, params: TDAParams) = {
    import params._

    val amendedCtx = params.amend(ctx)

    val levelSetsRDD = createLevelSets(lens, amendedCtx)
  }

  case class LSHLinkParams(phase: Int,
                           nrHashTables: Int,
                           k: Int,
                           r: Double,
                           A: Double = 1.4,
                           distanceSpec: HList = "manhattan" :: HNil) extends Serializable {
    // TODO verify only euclidean or manhattan distances.
  }

  case class Cluster(id: UUID = UUID.randomUUID(),
                     phase: Int) extends Serializable

  def maxCoordinate(points: Iterable[DataPoint]) = points.map(p => p.features(p.features.argmax)).max



  def LSH_Link(levelSetPoints: Iterable[DataPoint]) = {

    val f = 25
    val C = maxCoordinate(levelSetPoints)
    val r = C / f // bucketWidth
    val n = levelSetPoints.size
    val L = 50
    val k = 100

    val map = levelSetPoints.map(p => (p.index, p)).toMap



    def recur(phasePoints: Iterable[(DataPoint, Option[Cluster])],
              phaseParams: LSHLinkParams) = {

//      phasePoints
//        .flatMap(p => )

      ???
    }


    // val initPoints = levelSetPoints.map(p => (p, None[Cluster]))

    // val initParams = LSHLinkParams(0, L, k, r)

    ???
  }




//  def lshLink(levelSetPoints: Iterable[DataPoint],
//              f: Int) = {
//
//    val C = levelSetPoints.maxBy(_.features)
//
//    @tailrec
//    def recur(points: Iterable[(DataPoint, Option[Cluster])],
//              phaseParams: LSHLinkParams) = {
//
//      ???
//    }
//
//    recur(levelSetPoints.map(p => (p, None)), initParams)
//  }



  def clusterLevelSetsWithLSHLink(levelSetsRDD: RDD[(LevelSetID, DataPoint)],
                                  lshLinkParams: LSHLinkParams) = {

    levelSetsRDD
      .groupByKey
      .mapValues(levelSetPoints =>



        levelSetPoints

        )
      .cache
  }



}