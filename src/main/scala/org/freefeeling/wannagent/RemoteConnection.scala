package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor.{Stash, ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import akka.io.Tcp.{Close, Write, Register, Received}
import akka.io.{IO, Tcp}
import akka.remote.transport.ThrottlerTransportAdapter.Direction.Receive
import akka.util.ByteString
import org.log4s._
import spray.can.HttpRequestRender
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeader
import spray.http.HttpHeaders.`Cache-Control`

/**
  * Created by zh on 15-12-13.
  */
class RemoteConnection(proxy: ActorRef, private var addr: InetSocketAddress, private var firstRequest: HttpRequestWithOrigin) extends Actor with Stash {

  import RemoteConnection._
  import context.system

  val logger = getLogger(getClass)
  IO(Tcp) ! Tcp.Connect(addr)

  var server: ActorRef = _

  def connect: Receive = {
    case connected: Tcp.Connected =>
      this.server = sender()
      this.server ! Tcp.Register(self)
      logger.debug(s"create a new connection ${self.path} to ${addr}")
      self ! firstRequest
      context.become(transfer)
  }

  val parser = new RequestParser(system.settings.config)

  def handleRequest(origin: HttpRequestWithOrigin) = {
    Option(origin.request.uri) match {
      case Some(absoluteUri) =>
        val newRequest = origin.request.copy(
          uri = absoluteUri.toRelative
//          headers = (origin.request.headers ::: `Cache-Control`(Seq(`max-age`(0))) :: Nil)
        )
        val render = RequestRender(context.system.settings.config)
        render.renderRequest(newRequest, addr)
      case None =>
        origin.origin
    }
  }

  def transfer: Receive = {
    case request: HttpRequestWithOrigin =>
      val ensuredRequest = if(request.request == null) request.copy(request = parser.parseRequest(request.origin).request) else request
      // some times a browser like firefox may change remote server within the same connection
      if(ensuredRequest.host != addr) {
        logger.warn(s"remote connection ${self.path} changing address from ${addr} to ${ensuredRequest.host}")
        this.server ! Tcp.Close
        addr = ensuredRequest.host
        IO(Tcp) ! Tcp.Connect(addr)
        firstRequest = request
        context.become(connect)
      } else {
        val newRequest = handleRequest(ensuredRequest)
        //      val newRequest = request.origin
        logger.debug(s"sending request to ${addr} by ${self.path}\n${newRequest.utf8String}")
        this.server ! Tcp.Write(newRequest)
      }
    case Received(data) =>
      logger.debug(s"recieved response from ${addr}(${data.size}) \n ${data.slice(0, 1000).utf8String}")
      this.proxy ! Response(data)
  }

  override def receive: Receive = connect
}

object RemoteConnection {
  def apply(proxy: ActorRef, addr: InetSocketAddress, request: HttpRequestWithOrigin) = Props(classOf[RemoteConnection], proxy, addr, request)

  type Connected = Tcp.Connected

  case class Response(origin: ByteString) extends Tcp.Command

}
