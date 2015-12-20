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

  def test = {
    ByteString("GET /s?wd=b&rsv_bp=0&inputT=3659 HTTP/1.1\r\n" +
      "Host: www.baidu.com\r\n" +
      "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:43.0) Gecko/20100101 Firefox/43.0\r\n" +
      "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
      "Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3\r\n" +
      "Accept-Encoding: gzip, deflate\r\n" +
      "Referer: http://www.baidu.com/\r\n" +
      "Cookie: BAIDUID=75DA9210F6CC4F79EC42877BEDB35E32:FG=1; BIDUPSID=75DA9210F6CC4F79EC42877BEDB35E32; PSTM=1449665938; PSLOSTID=1034\r\n" +
      "Connection: keep-alive\r\n\r\n")
  }

  val parser = new RequestParser(system.settings.config)

  def handleRequest(origin: HttpRequestWithOrigin) = {
    Option(origin.request.uri) match {
      case Some(absoluteUri) =>
        val newRequest = origin.request.copy(
          uri = absoluteUri.toRelative
//          headers = (origin.request.headers ::: `Cache-Control`(Seq(`max-age`(0))) :: Nil)
        )
        val render = HttpRequestRender(context.system.settings.config)
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
        logger.warn(s"remote connection ${self.path} chaging address from ${addr} to ${ensuredRequest.host}")
        this.server ! Tcp.Close
        addr = ensuredRequest.host
        IO(Tcp) ! Tcp.Connect(addr)
        firstRequest = request
        context.become(connect)
      } else {
        logger.debug(s"origin request ${request.origin.utf8String}")
        val newRequest = handleRequest(ensuredRequest)
        //      val newRequest = request.origin
        logger.debug(s"sending request to ${addr} by ${self.path}\n${newRequest.utf8String}")
        this.server ! Tcp.Write(newRequest)
      }
    case Received(data) =>
      logger.debug(s"recieved response from ${addr} \n ${data.utf8String}")
      this.proxy ! Response(data)
  }

  override def receive: Receive = connect
}

object RemoteConnection {
  def apply(proxy: ActorRef, addr: InetSocketAddress, request: HttpRequestWithOrigin) = Props(classOf[RemoteConnection], proxy, addr, request)

  type Connected = Tcp.Connected

  case class Response(origin: ByteString) extends Tcp.Command

}
