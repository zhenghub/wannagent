package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.util.ByteString
import com.typesafe.config.Config
import spray.http.{HttpEntity}

/**
  * Created by zh on 15-12-27.
  */
class RequestRender(config: Config) {

  val CrLf = ByteString("\r\n")

  def renderRequest(request: HttpRequest, remote: InetSocketAddress) = {
    var res = ByteString(request.method + " " + request.uri + " " + request.protocol + "\r\n")
    request.headers.foreach { header =>
      res ++= ByteString(header.name)
      res ++= ByteString(": ")
      res ++= ByteString(header.value)
      res ++= CrLf
    }
    res ++= CrLf
    request.entity match {
      case HttpEntity.Empty =>
      case _ =>
        res ++= request.entity.data.toByteString
        //throw new RuntimeException(s"unkown http entity: ${request.entity}")
    }
    res
  }

}

object RequestRender {
  def apply(config: Config) = {
    new RequestRender(config)
  }
}
