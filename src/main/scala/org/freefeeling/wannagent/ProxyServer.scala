package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorSystem}
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import spray.can.Http
import org.log4s._

/**
  * Created by zh on 15-12-13.
  */
class ProxyServer(addr: InetSocketAddress) extends Actor {
  import ProxyServer.logger
  import context.system

  IO(Tcp) ! Tcp.Bind(self, addr)
  var id = 0L

  def receive = {
    case b@Bound(localAddress) =>
      logger.info(s"wannagent server bound to ${localAddress}")
    case CommandFailed(bf: Bind) =>
      logger.error(s"bound failed ${bf}")
      context stop self
    case c@Connected(remote, local) =>
      val handler = context.actorOf(ProxyConnection(remote), "con_" + id)
      logger.info(s"create a new connection ${handler.path} from ${remote}")
      logger.debug(s"current connection count: ${context.children.size}")
      id += 1
      val connection = sender()
      connection ! Register(handler)
    case msg =>
      logger.warn(s"unkown message ${msg}")
  }
}

object ProxyServer {

  val logger = getLogger(getClass)
  def apply(addr: InetSocketAddress) = Props(classOf[ProxyServer], addr)

  def main(args: Array[String]) {
    logger.info("----------------- starting wannagent -----------------")
    logger.info(s"classpath: ${System.getProperty("java.class.path")}")
    implicit val as = ActorSystem("proxy")
    val addr = as.settings.config.getString("wannagent.proxy.host") match {
      case "*" => new InetSocketAddress(as.settings.config.getInt("wannagent.proxy.port"))
      case host =>
        new InetSocketAddress(host, as.settings.config.getInt("wannagent.proxy.port"))
    }
    as.actorOf(ProxyServer(addr))
    logger.info("wannagent running")
  }
}