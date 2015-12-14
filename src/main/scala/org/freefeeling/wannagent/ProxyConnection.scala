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
  implicit val system = context.system
  val parser = HttpRequestParser(system.settings.config)

  override def receive: Receive = {
    case Received(data) =>
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
              context.actorOf(RemoteConnection(self, new InetSocketAddress(h, p)))
          }
          this.request = request
          this.client = sender()
      }
    case Tcp.Connected(remote, _) =>
      sender() ! this.request
    case response: RemoteConnection.Response =>
      this.client ! response.origin
    case msg =>
      println(msg)
  }
}

object ProxyConnection {
  def apply() = Props(classOf[ProxyConnection])
}
