package org.tmoerman.plongeur.tda

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model.{Filter, TDALens, _}
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.tda.cluster.Scale._
import rx.lang.scala.Observable
import shapeless.HNil

import scala.concurrent.duration._

/**
  * @author Thomas Moerman
  */
class MachineSpec extends FlatSpec with Matchers {

  val l10 = TDALens(
    Filter(Feature(0), 10, 0.5),
    Filter(Feature(1), 10, 0.5))

  val l05 = TDALens(
    Filter(Feature(0), 20, 0.5),
    Filter(Feature(1), 20, 0.5))

  val c = ClusteringParams()

  val h10 = histogram(10)

  val h12 = histogram(12)

  val p1 = TDAParams(lens = l10, clusteringParams = c, scaleSelection = h10)

  val p2 = TDAParams(lens = l10, clusteringParams = c, scaleSelection = h12)

  val p3 = TDAParams(lens = l05, clusteringParams = c, scaleSelection = h12)

  "lenses with different filters" should "not be equal" in {
    l10 should not be l05
  }

  "pushing identical parameters through the machine" should "yield one result" in {
    val o1 = Observable.just(p1).delay(100 millis)
    val o2 = Observable.just(p1).delay(200 millis)
    val o3 = Observable.just(p1).delay(300 millis)

    val cc = List(o1, o2, o3).reduce((a, b) => a.merge(b))

    runMachine(cc).toBlocking.toList shouldBe List((l10, c, h10))
  }

  "pushing different parameters through the machine" should "yield multiple results" in {
    val o1 = Observable.just(p1).delay(100 millis)
    val o2 = Observable.just(p2).delay(200 millis)
    val o3 = Observable.just(p3).delay(300 millis)

    val cc = List(o1, o2, o3).reduce((a, b) => a.merge(b))

    runMachine(cc).toBlocking.toList shouldBe List((l10, c, h10), (l10, c, h12), (l05, c, h12))
  }

  def runMachine(in$: Observable[TDAParams]) = {

    val lens$             = in$.map(_.lens).distinct.onBackpressureLatest
    val clusteringParams$ = in$.map(_.clusteringParams).distinct.onBackpressureLatest
    val scaleSelection$   = in$.map(_.scaleSelection).distinct.onBackpressureLatest

    lens$
      .combineLatest(clusteringParams$)
      .combineLatestWith(scaleSelection$){ case ((a, b), c) => (a, b, c) }
  }
}
