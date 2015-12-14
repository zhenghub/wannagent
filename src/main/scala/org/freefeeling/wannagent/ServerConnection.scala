package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import akka.io.Tcp.Received
import akka.io.{IO, Tcp}
import akka.remote.transport.ThrottlerTransportAdapter.Direction.Receive
import akka.util.ByteString
import org.freefeeling.wannagent.http.HttpRequestWithOrigin

/**
  * Created by zh on 15-12-13.
  */
class RemoteConnection(client: ActorRef, addr: InetSocketAddress) extends Actor{
  import RemoteConnection._
  import context.system
  IO(Tcp) ! Tcp.Connect(addr)

  var server: ActorRef = _
  override def receive: Receive = {
    case connected : Tcp.Connected =>
      client ! connected
      this.server = sender()
    case request: HttpRequestWithOrigin =>
      this.server ! request.origin
    case Received(data) =>
      this.client ! Response(data)
  }
}

object RemoteConnection {
  def apply(client: ActorRef, addr: InetSocketAddress) = Props(classOf[RemoteConnection], client, addr)

  type Connected = Tcp.Connected

  case class Response(origin: ByteString) extends Tcp.Command
}
