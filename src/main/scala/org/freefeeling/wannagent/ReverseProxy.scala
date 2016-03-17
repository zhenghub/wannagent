package org.freefeeling.wannagent

import java.io.PrintStream
import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor._
import akka.event.Logging
import akka.event.Logging.Error
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.{Timeout, ByteString}
import com.typesafe.config.{Config, ConfigFactory}
import org.freefeeling.wannagent.ClientSide.ClientMaster
import org.freefeeling.wannagent.ReverseProxy.{ServerData, ClientData, PortMap}
import org.freefeeling.wannagent.ServerSide.ServerController
import org.freefeeling.wannagent.common.{AkkaUtil, MapList}
import org.freefeeling.wannagent.distributed.{Msgs, Msg}
import org.freefeeling.wannagent.distributed.Msgs._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.pickling.shareNothing._
import scala.pickling.static._
import scala.pickling.binary._
import scala.pickling.Defaults._
import akka.pattern._

/**
  * Created by zhenghu on 16-3-12.
  */
object ReverseProxy {

  case class ClientData(data: ByteString)

  case class ServerData(data: ByteString)

  case class PortMap(remoteHost: String, remotePort: Int, localHost: String, localPort: Int) {
    def localAddr = s"${localHost}:${localPort}"
  }

  class Logging extends Actor with ActorLogging {
    override def receive: Actor.Receive = {
      case Error(cause, logSource, logClass, message) =>
        val causeString = {
          val builder = ByteString.newBuilder
          val bos = new PrintStream(builder.asOutputStream)
          cause.printStackTrace(bos)
          bos.close()
          builder.result()
        }.utf8String
        log.info(s"(${Option(causeString).getOrElse("unkown cause")}, ${Option(logSource).getOrElse("ukown logsource")}, ${Option(logClass).map(_.getName).getOrElse("unkown logClass")}, ${Option(message).map(_.toString).getOrElse("unkown message")}, ${System.currentTimeMillis()})")
    }
  }

  def monitor(as: ActorSystem): Unit = {
    as.eventStream.subscribe(as.actorOf(Props(classOf[Logging]), "monitor"), classOf[Logging.Error])
  }

  def startServer(config: Config): Unit = {
    val as = ActorSystem("rp_server", config)
    monitor(as)
    val host = config.getString("host")
    val port = config.getInt("port")
    val master = as.actorOf(Props(classOf[ServerController], host, port), "servercontroller")
    import scala.collection.convert.wrapAsScala._
    val map = config.getObject("portMap").entrySet().map { e =>
      val Array(lh, lp) = e.getKey.split(":")
      val Array(rh, rp) = e.getValue.unwrapped().asInstanceOf[String].split(":")
      PortMap(rh, rp.toInt, lh, lp.toInt)
    }
    map.foreach(master ! _)
  }

  def startClient(config: Config): Unit = {
    val as = ActorSystem("rp_client", config)
    monitor(as)
    val serverHost = config.getString("server_host")
    val serverPort = config.getInt("server_port")
    val hostid = config.getString("hostid")
    as.actorOf(Props(classOf[ClientMaster], hostid, serverHost, serverPort), "client")
  }

  class TestServe extends Actor with ActorLogging {
    implicit val as = context.system

    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(7788))

    override def receive: Actor.Receive = {
      case b@Bound(localAddress) =>
        log.info(s"servercontroller bound to ${localAddress}")
      case CommandFailed(bf: Bind) =>
        log.error(s"bound failed ${bf}")
        context stop self

      case c@Connected(remote, local) =>
        sender() ! Register(self)
        1 to 3 foreach { i =>
          sender() ! Tcp.Write(ByteString(s"server hello ${i}\n"))
          Thread.sleep(10)
        }
      case Received(data) =>
        log.info(data.utf8String)
        sender() ! Tcp.Write(ByteString(s"recieved ${data.utf8String}"))
      case PeerClosed =>
    }
  }

  def startTest(config: Config): Unit = {
    val as = ActorSystem("test")
    as.actorOf(Props(classOf[TestServe]))
  }

  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    args(0) match {
      case "server" =>
        startServer(config.getConfig("wannagent.reverseproxy.server").withFallback(config.getConfig("wannagent.reverseproxy")).withFallback(ConfigFactory.defaultReference()))
      case "client" =>
        startClient(config.getConfig("wannagent.reverseproxy.client").withFallback(ConfigFactory.defaultReference()))
      case "testserver" =>
        startTest(config)
    }
  }
}

object ClientSide {

  class ClientWorker2Server(val serverAddr: InetSocketAddress, tunnel: Tunnel, realConn: ActorRef) extends Actor with ActorLogging {
    implicit val as = context.system
    IO(Tcp) ! Tcp.Connect(serverAddr)
    private var server: ActorRef = null

    override def receive: Actor.Receive = {
      case connected: Tcp.Connected =>
        server = sender()
        server ! Tcp.Register(self)
        server ! Tcp.Write(tunnel.pickle)
        context.become(work)
        log.info("worker2server initialized")
    }

    def work: Receive = {
      case Received(data) =>
        realConn ! ServerData(data)
      case ClientData(data) =>
        server ! Tcp.Write(data)
        log.info(s"client send data to server: ${data.utf8String}")
      case PeerClosed =>
        context.stop(self)
    }

    override def postStop(): Unit = {
      if (server != null)
        server ! Close
    }

  }

  class ClientWorker(localPort: Int, serverAddr: InetSocketAddress, tunnel: Tunnel) extends Actor with ActorLogging {
    implicit val as = context.system
    val localAddr = new InetSocketAddress("127.0.0.1", localPort)
    IO(Tcp) ! Tcp.Connect(localAddr)
    var real:ActorRef = _
    var serverActor: ActorRef = _

    def work: Receive = {
      case Received(data) =>
        serverActor ! ClientData(data)
        log.info(s"real server send data: ${data.utf8String}")
      case ServerData(data) =>
        real ! Tcp.Write(data)
      case PeerClosed =>
        context.stop(self)
        log.info(s"stop connection ${self}")
      case t: Terminated =>
        context.stop(self)
        log.info(s"stop connection ${self}")
    }

    override def receive: Actor.Receive = {
      case connected: Tcp.Connected =>
        real = sender()
        real ! Tcp.Register(self)
        serverActor = context.actorOf(Props(classOf[ClientWorker2Server], serverAddr, tunnel, self))
        context.watch(serverActor)
        context.become(work)
    }
  }

  class Client4Server(host: String, serverHost: String, serverPort: Int) extends Actor with ActorLogging {
    implicit val as = context.system
    val serverAddr = new InetSocketAddress(serverHost, serverPort)
    IO(Tcp) ! Tcp.Connect(serverAddr)

    def connectServer: Receive = {
      case connected: Tcp.Connected =>
        val server = sender()
        server ! Tcp.Register(self)
        log.info(s"${host} connected to server: ${serverHost}:${serverPort}")
        server ! Tcp.Write(ClientId(host).pickle)

        def handler: Receive = {
          case Received(data) =>
            Msgs(data) match {
              case t@Tunnel(_, port, id) =>
                log.info(s"start to build connection from localhost:${port} to ${serverHost}:${serverPort}")
                context.watch(context.actorOf(Props(classOf[ClientWorker], port, serverAddr, t), "worker_" + System.currentTimeMillis()))
            }
          case Terminated(actor) =>
            log.info(s"${actor} stopped")
        }
        context.become(handler)
    }

    override def receive = connectServer
  }

  class ClientMaster(hostId: String, serverHost: String, serverPort: Int) extends Actor with ActorLogging {
    context.actorOf(Props(classOf[Client4Server], hostId, serverHost: String, serverPort: Int))

    override def receive: Actor.Receive = {
      case e =>
        log.info(s"${self.path} recieve: ${e}")
    }
  }

}

object ServerSide {

  case class NewConnection(hostId: String, port: Int)

  class ClientsConnection(remote: ActorRef) extends Actor with ActorLogging with Stash {

    log.info("new client connection built")

    def decideType: Receive = {
      case Received(data) =>
        val msg = Msgs(data)
        msg match {
          case ClientId(host) =>
            log.info(s"client ${host} activated")
            context.parent ! msg
            context.become(clientMan)
          case newTunnel: Tunnel =>
            log.info(s"client worker connection ${newTunnel.id}/${newTunnel.hostId}:${newTunnel.clientPort} activated")
            context.parent ! newTunnel
            context.become(preWork)
        }
    }

    def preWork: Receive = {
      case workFor: ActorRef =>
        val front = workFor
        unstashAll()
        def worker: Receive = {
          case Received(data) =>
            front ! ServerData(data)
            log.info(s"server receive data: ${data.utf8String}")
          case ClientData(data) =>
            remote ! Tcp.Write(data)
          case PeerClosed =>
            context.stop(self)
        }
        context.become(worker)
      case e: Received =>
        stash()
        log.error(s"maybe here missing msg: ${e}")
      case e =>
        log.error(s"unkown msg: ${e}")
    }

    def clientMan: Actor.Receive = {
      case tunnel: Tunnel =>
        log.info(s"ask client for a new tunnel: ${tunnel.hostId}")
        remote ! Tcp.Write(tunnel.pickle)
    }

    override def receive: Actor.Receive = decideType
  }

  class AddrManager {

    var addr2Server = Map[String, ActorRef]()
    var hostId2addr = MapList[String, PortMap]()
    var hostId2Client = Map[String, ActorRef]()

    def addAddr(pm: PortMap): Unit = {
      hostId2addr +^= (pm.remoteHost -> pm)
    }

    def enableClient(hostId: String)(implicit af: ActorRefFactory): Unit = {
      hostId2addr.get(hostId).get.foreach { pm =>
        val addr = pm.localAddr
        if (!addr2Server.contains(addr)) {
          addr2Server += addr -> af.actorOf(Props(classOf[Server], pm.remoteHost, pm.remotePort, new InetSocketAddress(pm.localHost, pm.localPort)))
        }
      }
    }

    def updateClientMan(hostId: String, clientMan: ActorRef)(implicit af: ActorRefFactory): Unit = {
      hostId2Client += hostId -> clientMan
      enableClient(hostId)
    }

    def sendMsg2Client(hostId: String, msg: AnyRef) {
      hostId2Client(hostId) ! msg
    }

  }

  class ConnectionBuilder {
    var connId = 0L
    var requests = Map[Long, ActorRef]()

    def newId = {
      val id = connId
      connId += 1
      id
    }

    def newConnectionRequest(addrMan: AddrManager, hostId: String, port: Int, server: ActorRef) = {
      val id = newId
      requests += id -> server
      addrMan.sendMsg2Client(hostId, Tunnel(hostId, port, id))
    }

    def newConnectionResp(tunnel: Tunnel, conn: ActorRef): Unit = {
      requests(tunnel.id) ! conn
      requests -= tunnel.id
    }

  }

  class ServerController(host: String, port: Int) extends Actor with ActorLogging {
    implicit val as = context.system

    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(host, port))

    val addrManager = new AddrManager
    val connBuilder = new ConnectionBuilder

    override def receive: Actor.Receive = {

      case b@Bound(localAddress) =>
        log.info(s"servercontroller bound to ${localAddress}")
      case CommandFailed(bf: Bind) =>
        log.error(s"bound failed ${bf}")
        context stop self

      case c@Connected(remote, local) =>
        val clientConn = context.actorOf(Props(classOf[ClientsConnection], sender()), "cc_" + System.currentTimeMillis())
        sender() ! Register(clientConn)
        context.watch(clientConn)

      case pm@PortMap(rh, rp, lh, lp) =>
        addrManager.addAddr(pm)

      case ClientId(hostId) =>
        addrManager.updateClientMan(hostId, sender())(context)

      case NewConnection(hostId, port) =>
        connBuilder.newConnectionRequest(addrManager, hostId, port, sender())

      case t: Tunnel =>
        connBuilder.newConnectionResp(t, sender())

      case Terminated(actor) =>
        log.info(s"${actor} stopped")
      case msg =>
        log.warning(s"unkown message ${msg}")
    }
  }

  class Server(hostId: String, port: Int, addr: InetSocketAddress) extends Actor with ActorLogging {
    implicit val as = context.system
    IO(Tcp) ! Tcp.Bind(self, addr)

    override def receive: Receive = {
      case b@Bound(localAddress) =>
        log.info(s"server bound to ${localAddress}")
      case CommandFailed(bf: Bind) =>
        log.error(s"bound failed ${bf}")
        context stop self
      case c@Connected(remote, local) =>
        val conn = AkkaUtil.waitRes[ActorRef](context.parent, NewConnection(hostId, port), 10)
        val frontWorker = context.actorOf(Props(classOf[FrontConnection], sender(), conn))
        conn ! frontWorker
        sender() ! Register(frontWorker)
        context.watch(frontWorker)
        log.info(s"${addr}'s current connections: ${context.children.size}")
      case Terminated(actorRef) =>
        log.info(s"${actorRef} stopped")
        log.info(s"${addr}'s current connections: ${context.children.size}")
      case msg =>
        log.warning(s"unkown message ${msg}")
    }
  }

  class FrontConnection(client: ActorRef, remote: ActorRef) extends Actor with ActorLogging {

    context.watch(remote)

    override def receive: Actor.Receive = {
      case Received(data) =>
        remote ! ClientData(data)
        log.info(s"send 2 remote: ${data.utf8String}")
      case ServerData(data) =>
        client ! Tcp.Write(data)
        log.info(s"send 2 client: ${data.utf8String}")
      case PeerClosed =>
        remote ! PeerClosed
        context.stop(self)
        log.info(s"stop connection ${self}")
      case Terminated(actor) =>
        if (actor == remote) {
          context.stop(self)
          log.info(s"stop connection ${self}")
        }
    }
  }

}
