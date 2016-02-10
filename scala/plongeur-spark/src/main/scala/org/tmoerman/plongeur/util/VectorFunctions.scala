package org.tmoerman.plongeur.util

/**
  * @author Thomas Moerman
  */
object VectorFunctions {

  implicit def pimpVector[T](v: Vector[T]): VectorFunctions[T] = new VectorFunctions[T](v)

}

class VectorFunctions[T](v: Vector[T]) extends Serializable {

  private class HasIndex[A](n: Int) extends (A => Boolean) {
    private[this] var i = -1
    def apply(a: A): Boolean = { i += 1; i==n }
  }

  def dropByIndex(n: Int): Vector[T] = v.filterNot(new HasIndex[T](n))

}
