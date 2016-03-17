package org.freefeeling.wannagent.common

import java.net.InetSocketAddress

/**
  * Created by zhenghu on 16-3-17.
  */
trait IFreeString {

  val str: String

  def toInetSocketAddress = {
    val sa = str.split(':')
    val port = if(str.length > 1) sa(1).toInt else 80
    val host = if(str(0) == "*") "0.0.0.0" else sa(0)
    new InetSocketAddress(host, port)
  }

}

class FreeString(val str: String) extends IFreeString

object IFreeString {
  implicit def str2FreeString(str: String) = new FreeString(str)
}