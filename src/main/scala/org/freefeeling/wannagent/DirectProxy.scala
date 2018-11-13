package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.TLSProtocol.NegotiateNewSession
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import javax.net.ssl.SSLContext
import org.freefeeling.wannagent.modules.ThroughputLogger

import scala.concurrent.Future

/**
  * Created by zh on 2017/5/17.
  */
object DirectProxy {

  case class Param(listenAddress: InetSocketAddress, forwardAddress: InetSocketAddress, forwardProtocol: String)

  def addressStr2Address(addr: String) = addr.split(":") match {
    case Array(host, port) => InetSocketAddress.createUnresolved(host, port.toInt)
    case _ => throw new RuntimeException(s"cannot resolve host and port from address ${addr}")
  }

  def apply(param: Param): Unit = {
    implicit val as = ActorSystem("direct-proxy")
    implicit val am = ActorMaterializer()

    val listenAddr = param.listenAddress
    val forwardAddr = param.forwardAddress
    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      akka.stream.scaladsl.Tcp().bind(listenAddr.getHostName, listenAddr.getPort)

    val outgoingConnection = param.forwardProtocol match {
      case "tcp" =>
        akka.stream.scaladsl.Tcp().outgoingConnection(forwardAddr.getHostName, forwardAddr.getPort)
      case "tls" =>
        akka.stream.scaladsl.Tcp().outgoingTlsConnection(forwardAddr, SSLContext.getDefault, NegotiateNewSession.withDefaults)
    }

    val recordThoughput = ThroughputLogger.apply().join(outgoingConnection)
    val binding = connections.runForeach(_.handleWith(recordThoughput))
  }

}
