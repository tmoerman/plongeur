package org.tmoerman.lab

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.tda.Model.{Percentage, Filter, TDALens}
import org.tmoerman.plongeur.tda.TDAParams
import org.tmoerman.plongeur.tda.TDAParams.{setFilterOverlap, setFilterNrBins}
import org.tmoerman.plongeur.tda.cluster.Clustering.ClusteringParams
import org.tmoerman.plongeur.tda.cluster.Scale
import org.tmoerman.plongeur.tda.cluster.Scale.histogram
import org.tmoerman.plongeur.util.RxUtils._
import rx.lang.scala.subjects.PublishSubject
import shapeless.HNil

/**
  * @author Thomas Moerman
  */
class RxLab extends FlatSpec with Matchers {

  behavior of "creating an observable from a function"

  it should "possible with a Subject" in {

    val in = PublishSubject[String]

    val put = putFn(in)

    val o = in.toVector

    o.subscribe{ _ shouldBe List("foo", "gee", "bar") }

    put("foo")
    put("gee")
    put("bar")
    in.onCompleted()

    waitFor(o)
  }

//  it should "be possible with the constructor" in {
//
//    val in = Observable.create{ o =>
//      Subscription()
//    }
//
//    val o = in.foldLeft(List[String]()){ (l, s) => s :: l }
//
//    o.subscribe{ _ shouldBe List("bar", "gee", "foo") }
//
//    put("foo")
//    put("gee")
//    put("bar")
//    in.onCompleted()
//
//    waitFor(o)
//  }

  behavior of "adding subjects to a mix"

  it should "work in a trivial case" in {

    val mix = PublishSubject[String]()
    val in1 = PublishSubject[String]()
    val in2 = PublishSubject[String]()

    val o = mix.toVector

    o.subscribe{ _ shouldBe Vector(
      "in1.foo",
      "in1.gee",
      "in2.foo",
      "in2.gee",
      "in1.bar"
    )}

    val s1 = in1.subscribe(mix)
    val s2 = in2.subscribe(mix)

    in1.onNext("in1.foo")
    in1.onNext("in1.gee")
    in2.onNext("in2.foo")
    in2.onNext("in2.gee")

    s2.unsubscribe()      // unsubscribing in2 from the mix should result in no-effect operations when calling onNext
    in1.onNext("in1.bar")
    in2.onNext("in2.bar")

    mix.onCompleted()

    waitFor(o)
  }

  behavior of "Data structure decomposition"

  it should "only yield changes" in {

    val in = PublishSubject[(Symbol, Symbol, Symbol)]

    val source = in //.distinct -> doesn't make a difference...

    val in1 = source.map(_._1).distinct
    val in2 = source.map(_._2).distinct
    val in3 = source.map(_._3).distinct

    val combo =
      in1
        .combineLatest(in2)
        .combineLatest(in3)
        .toVector

    combo.subscribe{ _ shouldBe Vector(
      (('A,'B),'C),
      (('X,'B),'C),
      (('X,'B),'X),
      (('X,'X),'X)
    )}

    in.onNext(('A, 'B, 'C))
    in.onNext(('A, 'B, 'C))
    in.onNext(('A, 'B, 'C))

    in.onNext(('X, 'B, 'C))
    in.onNext(('X, 'B, 'C))

    in.onNext(('X, 'B, 'X))
    in.onNext(('X, 'B, 'X))

    in.onNext(('X, 'X, 'X))

    in.onCompleted()

    waitFor(combo)
  }

  behavior of "Piping observables"

  it should "forward from on observable to a subject" in {
    val base =
      TDAParams(
        lens = TDALens(
          Filter("feature" :: 0 :: HNil, 10, 0.6)),
        clusteringParams = ClusteringParams(),
        scaleSelection = histogram(10))

    val in$ = PublishSubject[TDAParams]

    val updates$ = in$.toVector

    updates$.subscribe{ _.map(_.lens.filters(0).nrBins) shouldBe Vector(10, 10, 10, 20, 20, 30, 30) }

    val nrBins$ = PublishSubject[Int]

    val overlap$ = PublishSubject[Percentage]

    val updateParams$ =
      List(nrBins$.map(v => setFilterNrBins(0, v)),
           overlap$.map(v => setFilterOverlap(0, v)))
        .reduce(_ merge _)
        .scan(base)((params, fn) => fn(params))

    updateParams$.subscribe(in$)

    nrBins$.onNext(10)
    overlap$.onNext(.1)
    nrBins$.onNext(20)
    overlap$.onNext(.2)
    nrBins$.onNext(30)
    overlap$.onNext(.3)
    nrBins$.onCompleted()
    overlap$.onCompleted()

    waitFor(updates$)
  }

}