package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{Tcp, IO}
import akka.io.Tcp._
import akka.util.ByteString
import org.freefeeling.wannagent.ProxyConnection.Ack
import spray.http._
import org.log4s._

import scala.util.{Failure, Success, Try}

/**
  * Created by zh on 15-12-13.
  */
class ProxyConnection(remoteAddr: InetSocketAddress) extends Actor with ActorLogging {
  val logger = getLogger(getClass)
  var client: ActorRef = _
  var requestCount = 0
  var remote: Option[ActorRef] = None
  implicit val system = context.system

  val parser = new RequestParser(system.settings.config)

  def extractMethodAndHost(request: HttpRequestWithOrigin) = {
    request match {
      case HttpRequestWithOrigin(_, HttpRequest(method, uri, headers, _, _)) =>
        logger.debug(s"request for ${method} ${uri}")
        this.client = sender()
        headers.find(header => header.name == "Host") match {
          case None =>
            throw new RuntimeException("no host found")
          case Some(host) =>
            val addr = host.value.split(":")
            val (h, p) = if (addr.length < 2) (addr(0), 80) else (addr(0), addr(1).toInt)
            (request.request.method, new InetSocketAddress(h, p))
        }
    }
  }

  /**
    * parse the first request and decide what kind of connection to use to connect the target server
    * @return
    */
  def selectConnectionType: Receive = {
    case Received(data) =>
      requestCount += 1
      logger.debug(s"${self.path} processing ${requestCount} request")
      logger.debug(s"""receive a connection request from ${remoteAddr} "
        ${data.utf8String}
        """")
      this.client = sender()
      val request = Try(parser.parseRequest(data)) match {
        case Success(request) => request
        case Failure(e) =>
          logger.error(e)(s"parse request error when parsing for ${remoteAddr} ${data}")
          throw e
      }
      val method = request.method
      val address = request.host
      remote = method match {
        case HttpMethods.CONNECT =>
          Option(context.actorOf(SecureRemoteConnection(self, address)))
        case _ =>
          Option(context.actorOf(RemoteConnection(self, address, request)))
      }
      context.become(forwardMessage)
    case PeerClosed =>
      logger.info(s"peer closed ${self}")
      handleclose
    case e: ErrorClosed =>
      logger.error(s"error closed ${e}")
      handleclose
    case msg =>
      logger.warn(s"unkown message ${msg}")
  }

  def handleclose = {
    context stop self
    logger.info(s"closing connection ${self.path}")
  }

  /**
    * send to the remote connection the request from the client;
    * send back to the client the response fetched by the remote connection
    * @return
    */
  def forwardMessage: Receive = {
    case Received(data) =>
      this.remote.get ! HttpRequestWithOrigin(data, null)
    case Ack =>
      this.remote.get ! Ack
    case response: RemoteConnection.Response =>
      this.client ! Write(response.origin, Ack)
    case PeerClosed =>
      log.info(s"peer closed ${self}")
      handleclose
    case e: ErrorClosed =>
      log.error(s"error closed ${e}")
      handleclose
    case CommandFailed(Write(resp, ack)) =>
      logger.error(s"send response back to client ${client} failed: ${ack}")
      this.client ! Write(resp, Ack)
      logger.info("retrying send response to client")
    case msg =>
      logger.warn(s"unkown message ${msg} from ${sender()}")
  }

  def life: Receive = {
    case "Kill" =>
      context.stop(self)
    case "Reconnect" =>
    case "Resp(msg)" =>
  }

  def close: Receive = {
    case _: ConnectionClosed =>
      context stop self
      logger.info(s"closing connection ${self.path}")
    case msg =>
      logger.warn(s"received a message while closing: ${msg}")
  }

  override def receive: Receive = selectConnectionType
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

  def apply(remote: InetSocketAddress) = Props(classOf[ProxyConnection], remote)

  object Ack extends Event

}