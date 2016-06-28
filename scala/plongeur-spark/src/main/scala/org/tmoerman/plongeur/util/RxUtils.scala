package org.tmoerman.plongeur.util

import rx.lang.scala.{Observable, Subject}

/**
  * @author Thomas Moerman
  */
object RxUtils {

  def putFn[T](s: Subject[T]) = (t: T) => s.onNext(t)

  def waitFor[T](obs: Observable[T]): Unit = {
    obs.toBlocking.toIterable.last
  }

}