package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.{Actor, Props, ActorRef}
import akka.io.Tcp.Received
import akka.io.{IO, Tcp}
import akka.util.ByteString
import org.freefeeling.wannagent.RemoteConnection.Response
import org.log4s._

/**
  * Created by zh on 15-12-20.
  */
class SecureRemoteConnection (proxy: ActorRef, addr: InetSocketAddress) extends Actor{
  import SecureRemoteConnection._
  import context.system
  val logger = getLogger(getClass)
  IO(Tcp) ! Tcp.Connect(addr)

  var server: ActorRef = _
  override def receive: Receive = {
    case connected : Tcp.Connected =>
      this.server = sender()
      this.server ! Tcp.Register(self)
      this.proxy ! connectedResponse
    case request: HttpRequestWithOrigin =>
      this.server ! Tcp.Write(request.origin)
    case Received(data) =>
      logger.debug(s"recieved response from ${addr}")
      this.proxy ! Response(data)
  }
}

object SecureRemoteConnection{
  def apply(proxy: ActorRef, addr: InetSocketAddress) = Props(classOf[SecureRemoteConnection], proxy, addr)

  val connectedResponse = Response(ByteString("HTTP/1.1 200 Connection established\r\nProxy-agent: wannagent-proxy/1.1\r\n\r\n"))

  type Connected = Tcp.Connected

}
