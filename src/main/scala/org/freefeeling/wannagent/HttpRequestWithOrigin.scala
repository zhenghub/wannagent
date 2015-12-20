package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.util.ByteString
import spray.http._

/**
  * Created by zh on 15-12-13.
  */
case class HttpRequestWithOrigin(val origin: ByteString, val request: HttpRequest) {
  def method = request.method

  lazy val host = request.headers.find(header => header.name == "Host") match {
    case None =>
      throw new RuntimeException("no host found")
    case Some(host) =>
      val addr = host.value.split(":")
      val (h, p) = if (addr.length < 2) (addr(0), 80) else (addr(0), addr(1).toInt)
      new InetSocketAddress(h, p)
  }
}
