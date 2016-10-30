package org.tmoerman.plongeur.util

import java.io.Serializable
import java.util.{PriorityQueue => JPriorityQueue}

import scala.collection.JavaConverters._
import scala.collection.generic.Growable

/**
  * Copied shamelessly from:
  *   https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/util/BoundedPriorityQueue.scala
  *
  * Added unique constraint.
  *
  * Bounded priority queue. This class wraps the original PriorityQueue
  * class and modifies it such that only the top K elements are retained.
  * The top K elements are defined by an implicit Ordering[A].
  */
class BoundedPriorityQueue[T](maxSize: Int, unique: Boolean = true)
                             (implicit ord: Ordering[T])
  extends Iterable[T]
     with Growable[T]
     with Serializable {

  private val underlying = new JPriorityQueue[T](maxSize, ord)

  override def iterator: Iterator[T] = underlying.iterator.asScala

  override def size: Int = underlying.size

  override def ++= (xs: TraversableOnce[T]): this.type = {
    xs.foreach { this += _ }
    this
  }

  override def += (elem: T): this.type = {
    if (size < maxSize) {
      underlying.offer(elem)
    } else {
      maybeReplaceLowest(elem)
    }

    this
  }

  override def +=(elem1: T, elem2: T, elems: T*): this.type = {
    this += elem1 += elem2 ++= elems
  }

  override def clear() { underlying.clear() }

  private def maybeReplaceLowest(elem: T): Boolean = {
    val head = underlying.peek()

    if (canOffer(elem, head)) {
      underlying.poll()
      underlying.offer(elem)
    } else {
      false
    }
  }

  private def canOffer(elem: T, head: T): Boolean = {
    head != null && ord.gt(elem, head) && ! (unique && underlying.contains(elem))
  }

}