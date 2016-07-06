package org.tmoerman.lab

import org.scalatest.{FlatSpec, Matchers}
import org.tmoerman.plongeur.util.RxUtils._
import rx.lang.scala.Observable
import rx.lang.scala.subjects.PublishSubject

/**
  * @author Thomas Moerman
  */
class RxLab extends FlatSpec with Matchers {

  behavior of "creating an observable from a function"

  it should "possible with a Subject" in {

    val in = PublishSubject[String]

    val put = putFn(in)

    val o = in.foldLeft(List[String]()){ (l, s) => s :: l }

    o.subscribe{ _ shouldBe List("bar", "gee", "foo") }

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

    val o = mix.foldLeft(List[String]()){ (l, s) => s :: l }

    o.subscribe{ _ shouldBe List("in1.bar", "in2.gee", "in2.foo", "in1.gee", "in1.foo") }

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

}