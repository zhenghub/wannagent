package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorSystem}
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import spray.can.Http

/**
  * Created by zh on 15-12-13.
  */
class ProxyServer(addr: InetSocketAddress) extends Actor {

  import context.system

  IO(Tcp) ! Tcp.Bind(self, addr)

  def receive = {
    case b@Bound(localAddress) =>
      println(s"bound to ${localAddress}")
    case CommandFailed(bf: Bind) =>
      println(s"bound failed ${bf}")
      context stop self
    case c@Connected(remote, local) =>
      println(s"connected from ${remote}")
      val handler = context.actorOf(ProxyConnection())
      val connection = sender()
      connection ! Register(handler)
    case msg =>
      println(msg)
  }
}

object ProxyServer {

  def apply(addr: InetSocketAddress) = Props(classOf[ProxyServer], addr)

  def main(args: Array[String]) {
    implicit val as = ActorSystem("proxy")
    val addr = new InetSocketAddress(as.settings.config.getString("wannagent.proxy.host"), as.settings.config.getInt("wannagent.proxy.port"))
    as.actorOf(ProxyServer(addr))
    println("wannagent running")
  }
}
