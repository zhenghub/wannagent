package org.freefeeling.wannagent.common

/**
  * Created by zhenghu on 16-3-17.
  */
class MapList[K, V](val underlying: Map[K, List[V]]) {

  def +^(element: (K, V)) = {
    this.get(element._1) match {
      case None =>
        this + (element._1 -> List(element._2))
      case Some(existed) =>
        this + (element._1 -> (element._2 :: existed))
    }
  }

  def +(kv: (K, List[V])) = {
    val nu = underlying + kv
    new MapList[K, V](nu)
  }

  def get(key: K): Option[List[V]] = underlying.get(key)

  def iterator: Iterator[(K, List[V])] = underlying.iterator

  def -(key: K) = new MapList(underlying - key)

  def result = underlying
}

object MapList {
  def apply[K, V]() = new MapList[K, V](Map[K, List[V]]())
}
