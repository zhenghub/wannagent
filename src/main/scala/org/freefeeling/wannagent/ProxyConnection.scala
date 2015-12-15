package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import akka.io.{Tcp, IO}
import akka.io.Tcp.{Write, Connected, Received}
import akka.util.ByteString
import org.freefeeling.wannagent.http.HttpRequestWithOrigin
import spray.can.parsing.HttpRequestParser
import spray.http._
import spray.http.parser.HttpParser

/**
  * Created by zh on 15-12-13.
  */
class ProxyConnection extends Actor {
  var request: HttpRequestWithOrigin = _
  var client: ActorRef = _
  var remote: Option[ActorRef] = None
  implicit val system = context.system

  import ProxyConnection._

  val parser = HttpRequestParser(system.settings.config)

  override def receive: Receive = {
    case Received(data) =>
      remote match {
        case None =>
        request = parser.parseRequest(data)
        request match {
          case HttpRequestWithOrigin(_, HttpRequest(method, uri, headers, _, _)) =>
            println(s"request for ${method} ${uri}")
            headers.find(header => header.name == "Host") match {
              case None =>
                throw new RuntimeException("no host found")
              case Some(host) =>
                val addr = host.value.split(":")
                val (h, p) = if (addr.length < 2) (addr(0), 80) else (addr(0), addr(1).toInt)
                this.remote = Option(context.actorOf(RemoteConnection(self, new InetSocketAddress(h, p))))
            }
            this.request = request
            this.client = sender()
        }
        case Some(remote) =>
          remote ! HttpRequestWithOrigin(data, null)
      }
    case Tcp.Connected(remote, _) =>
      if (this.request.request.method == HttpMethods.CONNECT) {
        this.client ! Write(ByteString(connectedResponse(this.request.request.protocol)))
      } else {
        sender() ! this.request
      }
    case response: RemoteConnection.Response =>
      this.client ! Write(response.origin)
    case msg =>
      println(msg)
  }
}

object ProxyConnection {
  def connectedResponse(protocol: HttpProtocol) = {
    protocol match {
      case HttpProtocols.`HTTP/1.0` =>
        "HTTP/1.0 200 Connection established\r\nProxy-agent: wannagent-proxy/1.1\r\n\r\n"
      case HttpProtocols.`HTTP/1.1` =>
        "HTTP/1.1 200 Connection established\r\nProxy-agent: wannagent-proxy/1.1\r\n\r\n"
    }
  }

  def apply() = Props(classOf[ProxyConnection])
}
