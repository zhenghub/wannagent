package org.freefeeling.wannagent.http

import akka.util.ByteString
import org.freefeeling.wannagent.http.HttpThings.{HttpProtocol, HttpProtocols, HttpRequest}

/**
  * Created by zh on 17-2-28.
  */
object HttpProxyProtocol {

  val `HTTP/1.0Response` = ByteString("HTTP/1.0 200 Connection established\r\nProxy-agent: wannagent-proxy/1.1\r\n\r\n")
  val `HTTP/1.1Response` = ByteString("HTTP/1.1 200 Connection established\r\nProxy-agent: wannagent-proxy/1.1\r\n\r\n")
  def connectedResponse(protocol: HttpProtocol) = {
    protocol match {
      case HttpProtocols.`HTTP/1.0` =>
        `HTTP/1.0Response`
      case HttpProtocols.`HTTP/1.1` =>
        `HTTP/1.1Response`
    }
  }

  object RequestRender {

    val CrLf = ByteString("\r\n")

    def renderRequest(request: HttpRequest) = {
      var res = ByteString(request.method + " " + request.uri.toRelative + " " + request.protocol + "\r\n")
      request.headers.foreach { header =>
        if(header.name == "Proxy-Connection") {
          res ++= ByteString("Connection")
        } else {
          res ++= ByteString(header.name)
        }
        res ++= ByteString(": ")
        res ++= ByteString(header.value)
        res ++= CrLf
      }
      res ++= CrLf
      request.entity match {
        case Iterator.empty =>
        case _ =>
          res ++= request.entity.reduce(_ ++ _)
      }
      res
    }

  }

}
