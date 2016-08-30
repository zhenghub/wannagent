package org.freefeeling.wannagent

import java.io.{FileInputStream, InputStream}
import java.net.InetSocketAddress
import java.security.KeyStore

import akka.actor.{ActorSystem, ActorRef, Props, Actor}
import akka.http.scaladsl.Http
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import akka.stream.ActorMaterializer
import akka.stream.TLSProtocol.{SessionBytes, SendBytes}
import akka.stream.scaladsl.{TLS, Keep, Flow, Source}
import akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import akka.util.ByteString
import org.freefeeling.wannagent.RemoteConnection.Response
import org.log4s._
import spray.http.{HttpMethods, HttpProtocol, HttpProtocols}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by zhenghu on 16-8-5.
  */
object BaseServer {

  class ProxyServer(addr: InetSocketAddress, remoteAddr: InetSocketAddress) extends Actor {

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
        val connection = sender()
        val handler = context.actorOf(ProxyConnection(connection, remoteAddr), "con_" + id)
        logger.info(s"create a new connection ${handler.path} from ${remote}")
        logger.debug(s"current connection count: ${context.children.size}")
        id += 1
        connection ! Register(handler)
      case msg =>
        logger.warn(s"unkown message ${msg}")
    }
  }

  class ProxyConnection(client: ActorRef, remoteAddr: InetSocketAddress) extends Actor {
    val logger = getLogger(getClass)
    var requestCount = 0
    var remote: Option[ActorRef] = Option(context.actorOf(Props(classOf[BaseRemoteConnection], self, remoteAddr)))
    implicit val system = context.system

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
        logger.info(s"peer closed ${self}")
        handleclose
      case e: ErrorClosed =>
        logger.error(s"error closed ${e}")
        handleclose
      case CommandFailed(Write(resp, ack)) =>
        logger.error(s"send response back to client ${client} failed: ${ack}")
        this.client ! Write(resp, Ack)
        logger.info("retrying send response to client")
      case msg =>
        logger.warn(s"unkown message ${msg} from ${sender()}")
    }

    def close: Receive = {
      case _: ConnectionClosed =>
        context stop self
        logger.info(s"closing connection ${self.path}")
      case msg =>
        logger.warn(s"received a message while closing: ${msg}")
    }

    override def receive: Receive = forwardMessage
  }

  object Ack extends Event

  object ProxyConnection {
    def apply(client: ActorRef, remoteAddr: InetSocketAddress) = Props(classOf[ProxyConnection], client, remoteAddr)
  }

  def current = System.currentTimeMillis()

  class BaseRemoteConnection(proxy: ActorRef, addr: InetSocketAddress) extends Actor {

    import context.system

    val logger = getLogger(getClass)
    IO(Tcp) ! Tcp.Connect(addr, pullMode = true)
    var requestId = 0L
    var lastRequestTime = current

    var server: ActorRef = _

    override def receive: Receive = {
      case connected: Tcp.Connected =>
        this.server = sender()
        this.server ! Tcp.Register(self)
        this.server ! ResumeReading
        logger.debug(s"create a new connection ${self.path} to ${addr}")
      case request: HttpRequestWithOrigin =>
        val data = request.origin
        requestId += 1L
        logger.debug(s"send request(${requestId}) from ${proxy}[${data.length}]")
        this.server ! Tcp.Write(data)
        this.server ! ResumeReading
        this.lastRequestTime = current
      case Ack =>
        this.server ! ResumeReading
      case Received(data) =>
        logger.debug(s"recieved response(time:${current - lastRequestTime}) from ${addr}[${data.length}]")
        this.proxy ! Response(data)
      case PeerClosed =>
        logger.debug("remote stopped")
        this.context.stop(self)
      case o =>
        logger.error(s"unkown message: ${o}")
    }
  }

  def actorVersion(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("tunnel")
    val port = args(0).toInt
    val (remoteHost, remotePort) = {
      val remote = args(1).split(":")
      (remote(0), remote(1).toInt)
    }
    actorSystem.actorOf(Props(classOf[ProxyServer], new InetSocketAddress(port), new InetSocketAddress(remoteHost, remotePort)))
  }

  def streamVersion(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("tunnel")
    implicit val materializer = ActorMaterializer()
    val port = args(0).toInt
    val (remoteHost, remotePort) = {
      val remote = args(1).split(":")
      (remote(0), remote(1).toInt)
    }
    val sslStage = {
      val isSsl = args.length > 1 && args(1) == "ssl"
      if(isSsl) {
        val password = args(2).toArray
        val keystore = new FileInputStream(args(3))
        val stage = Some(sslLayer(password, keystore))
        keystore.close()
        stage
      } else None
    }

    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      akka.stream.scaladsl.Tcp().bind("0.0.0.0", port)
    connections runForeach { connection =>
      val remote = akka.stream.scaladsl.Tcp().outgoingConnection(remoteHost, remotePort)

      val graph = sslStage match {
        case Some(ssl) =>
          val sslFlow = Flow[ByteString].map(SendBytes).via(ssl.join(connection.flow)).collect { case x: SessionBytes ⇒ x.bytes }
          sslFlow.joinMat(remote)(Keep.right)
        case None =>
          connection.flow.joinMat(remote)(Keep.right)
      }
      graph.run()
    }
  }

  def sslLayer(password: Array[Char], keystore: InputStream) = {
    import java.security.{SecureRandom, KeyStore}
    import javax.net.ssl.{SSLContext, TrustManagerFactory, KeyManagerFactory}

    import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext, Http}

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    //val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)
    TLS(https.sslContext, https.sslConfig, https.firstSession, akka.stream.Server, hostInfo = None)
  }

  def main(args: Array[String]) {
    streamVersion(args)
  }


}
