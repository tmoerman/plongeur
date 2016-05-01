package org.tmoerman.plongeur.util

/**
  * @author Thomas Moerman
  */
object MapFunctions {

  implicit def pimpMap[K, V](m: Map[K, V]): MapFunctions[K, V] = new MapFunctions(m)

}

class MapFunctions[K, V](val m: Map[K, V]) extends Serializable {

  /**
    * @param f A function that takes 2 values for the same key and merges them into a new value.
    * @param other
    * @return Returns a Map.
    */
  def merge(f: (V, V) => V)(other: Map[K, V]) =
    m.foldLeft(other){ case (m, (k, v)) => m + (k -> m.get(k).map(f(_, v)).getOrElse(v))}

}