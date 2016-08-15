package org.tmoerman.plongeur.tda

import java.util.concurrent.TimeUnit.SECONDS

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Inspections._
import org.tmoerman.plongeur.tda.Model.TDAParams._
import org.tmoerman.plongeur.tda.Model._
import org.tmoerman.plongeur.tda.cluster.Clustering._
import org.tmoerman.plongeur.tda.cluster.Scale._
import org.tmoerman.plongeur.test.{SparkContextSpec, TestResources}
import org.tmoerman.plongeur.util.RxUtils._
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject
import shapeless.HNil

import scala.concurrent.duration.Duration

/**
  * @author Thomas Moerman
  */
class TDAMachineSpec extends FlatSpec with SparkContextSpec with TestResources with Matchers {

  behavior of "TDA Machine"

  implicit val counter = mapToInt

  def printInspections(result: TDAResult, name: String) = {
    println(
      Seq(result.levelSetsToClusters.mkString("\n"),
          result.pointsToClusters.mkString("\n"),
          result.dotGraph(name))
        .mkString("\n"))
  }

  it should "work with one input" in {
    val inParams =
      TDAParams(
        lens = TDALens(Filter("feature" :: 0 :: HNil, 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val (outParams, result) =
      TDAMachine.run(TDAContext(sc, circle250RDD), Observable.just(inParams))
        .toBlocking
        .single

    inParams shouldBe outParams

    // printInspections(result, "test TDA Machine 1 input")
  }

  it should "work with one amending input" in {
    val inParams =
      TDAParams(
        lens = TDALens(Filter("PCA" :: 0 :: HNil, 10, 0.5)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val (outParams, result) =
      TDAMachine.run(TDAContext(sc, circle250RDD), Observable.just(inParams))
        .toBlocking
        .single

    inParams shouldBe outParams

    // printInspections(result, "test TDA Machine 1 input")
  }

  val secs_10 = Duration(10, SECONDS)

  val params_1 =
    TDAParams(
      lens = TDALens(
        Filter("feature" :: 0 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  val params_2 =
    TDAParams(
      lens = TDALens(
        Filter("feature" :: 1 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  val params_2_b =
    TDAParams(
      lens = TDALens(
        Filter("feature" :: 1 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(20))

  val params_3 = // parameters that change the context
    TDAParams(
      lens = TDALens(
        Filter("PCA" :: 0 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  val params_3_b =
    TDAParams(
      lens = TDALens(
        Filter("PCA" :: 0 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(20))

  val params_4 =
    TDAParams(
      lens = TDALens(
        Filter("eccentricity" :: "infinity" :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  val params_4_b =
    TDAParams(
      lens = TDALens(
        Filter("eccentricity" :: "infinity" :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(20))

  val map =
    Map(
      params_1    -> "params_1",
      params_2    -> "params_2",
      params_2_b  -> "params_2_b",
      params_3    -> "params_3",
      params_3_b  -> "params_3_b",
      params_4    -> "params_4",
      params_4_b  -> "params_4_b"
    )

  it should "work with repeated inputs" in {
    val in = PublishSubject[TDAParams]

    val ctx = TDAContext(sc, circle1kRDD)

    val out = TDAMachine.run(ctx, in).toVector

    out.subscribe(results => {
      println()
      println(results.map{ case (params, _) => map(params) }.mkString("\n"))

      results.map(_._1).toList shouldBe List(
        params_1,
        params_2,
        params_1,
        params_2,
        params_1,
        params_3,
        params_4,
        params_3, // TODO accept this behaviour for now... we
        params_3_b,
        params_2,
        params_2_b,
        params_4,
        params_4_b)
    })

    in.onNext(params_1)
    in.onNext(params_1)
    in.onNext(params_2)
    in.onNext(params_2)
    in.onNext(params_1)
    in.onNext(params_2)
    in.onNext(params_1)
    in.onNext(params_1)
    in.onNext(params_3)
    in.onNext(params_4)
    in.onNext(params_4)
    in.onNext(params_3_b)
    in.onNext(params_2_b)
    in.onNext(params_4_b)
    in.onCompleted()

    waitFor(out).map(_._2.clusters)
  }

  val p_pca_0 =
    TDAParams(
      lens = TDALens(
        Filter("PCA" :: 0 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  it should "work with pca filters" in {
    val in = PublishSubject[TDAParams]

    val ctx = TDAContext(sc, circle1kRDD)

    val out = TDAMachine.run(ctx, in).toVector

    val out_sub = out.subscribe(_.size shouldBe 3)

    in.onNext(p_pca_0)
    in.onNext(setFilterNrBins(0, 20)(p_pca_0))
    in.onNext(setFilterNrBins(0, 40)(p_pca_0))

    in.onCompleted()

    waitFor(out).map(_._2.clusters)
  }

  val p_ecc_1 =
    TDAParams(
      lens = TDALens(
        Filter("eccentricity" :: 1 :: HNil, 10, 0.5)),
      clusteringParams = ClusteringParams(),
      scaleSelection = histogram(10))

  it should "work with ecc 1 filter" in {
    val in = PublishSubject[TDAParams]

    val ctx = TDAContext(sc, circle1kRDD)

    val out = TDAMachine.run(ctx, in).toVector

    in.onNext(p_ecc_1)
    in.onNext(setFilterNrBins(0, 20)(p_ecc_1))
    in.onCompleted()

    waitFor(out).map(_._2.clusters)
  }

  it should "work with mixed filters" in {
    val in = PublishSubject[TDAParams]

    val ctx = TDAContext(sc, circle1kRDD)

    val out = TDAMachine.run(ctx, in).toVector

    in.onNext(p_ecc_1)
    in.onNext(setFilterNrBins(0, 20)(p_ecc_1))
    in.onNext(p_pca_0)
    in.onCompleted()

    waitFor(out).map(_._2.clusters)
  }

  behavior of "assocFilterMemos"

  it should "add memo entries to a TDAContext" in {
    val ctx = TDAContext(sc, circle1kRDD)

    val updated = p_pca_0.amend(ctx)

    ctx should not be updated

    val updated2 = p_pca_0.amend(updated)

    updated shouldBe updated2
  }



//  it should "work in dryRun" in {
//    val in = PublishSubject[TDAParams]
//
//    val out = dryRun(in).toVector
//
//    //val latch = new CountDownLatch(1)
//
//    val out_sub = out.subscribe(onNext = (t) => {
//      println(t)
//
//      t.size shouldBe 5
//      //latch.countDown()
//    })
//
//    in.onNext(params_1)
//    in.onNext(params_2)
//    in.onNext(params_1)
//    in.onNext(params_2)
//    in.onNext(params_1)
//    in.onCompleted()
//
//    waitFor(out)
//  }
//
//  def dryRun(tdaParam$: Observable[TDAParams]) = {
//
//    // source observable with backpressure
//
//    val tdaParamsSource$ = tdaParam$//.distinct
//
//    // deconstructing the parameters
//
//    val lens$               = tdaParamsSource$.map(_.lens                     ).distinctUntilChanged
//    val clusteringParams$   = tdaParamsSource$.map(_.clusteringParams         ).distinctUntilChanged
//    val scaleSelection$     = tdaParamsSource$.map(_.scaleSelection           ).distinct//.map(bla)
//    val collapseDuplicates$ = tdaParamsSource$.map(_.collapseDuplicateClusters).distinct//.map(bla)
//
//    // TDA computation merges in parameter changes
//
//    //val ctx$                 = Observable.just(TDAContext(null, null))
//
//    lens$
//      //.combineLatestWith(ctx$)((lens, ctx) => (lens, lens.assocFilterMemos(ctx)) )
//      //.combineLatest(clusteringParams$)
//      //.combineLatest(scaleSelection$)
//      //.combineLatest(collapseDuplicates$)
//  }

}