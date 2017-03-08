package org.freefeeling.wannagent.http

import akka.util.ByteString

/**
  * Created by zh on 17-2-28.
  */
object HttpThings {

  sealed case class HttpMethod(value: String) {
    override def toString = value
  }

  object HttpMethods {

    object CONNECT extends HttpMethod("CONNECT")

    def apply(method: String) = method match {
      case "CONNECT" => CONNECT
      case _ => HttpMethod(method)
    }

  }

  sealed class HttpHeader(val name: String, val value: String)

  object HttpHeader {
    def apply(name: String, value: String): HttpHeader = new HttpHeader(name, value)
  }

  case class TransferCoding(override val value: String) extends HttpHeader("transfer-coding", value)

  case class HttpProtocol(value: String) {
    override def toString = value
  }

  object HttpProtocols {
    val `HTTP/1.0` = HttpProtocol("HTTP/1.0")
    val `HTTP/1.1` = HttpProtocol("HTTP/1.1")
    val `HTTP/2.0` = HttpProtocol("HTTP/2.0")

    val protocols = Seq(`HTTP/1.0`, `HTTP/1.1`, `HTTP/2.0`).map(p => p.value -> p).toMap

    def apply(value: String) = protocols.apply(value)
  }

  type HttpEntity = Iterator[ByteString]

  case class HttpRequest(method: HttpMethod,
                         protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`,
                         uri: Uri = Uri./,
                         headers: List[HttpHeader] = Nil,
                         entity: HttpEntity)

}
