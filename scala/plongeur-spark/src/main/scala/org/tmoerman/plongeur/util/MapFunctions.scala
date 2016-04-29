package org.tmoerman.plongeur.util

/**
  * @author Thomas Moerman
  */
object MapFunctions {

  implicit def pimpMap[K, V](m: Map[K, V]): MapFunctions[K, V] = new MapFunctions(m)

}

class MapFunctions[K, V](val m: Map[K, V]) extends Serializable {

  def merge(f: (V, V) => V)(other: Map[K, V]) =
    m.foldLeft(other){ case (m, (k, v)) => m + (k -> m.get(k).map(f(_, v)).getOrElse(v))}

}