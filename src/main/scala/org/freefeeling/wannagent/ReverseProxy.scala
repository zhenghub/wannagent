package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.Actor.Receive
import akka.actor._
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.pickling.shareNothing._
import scala.pickling.static._
import scala.pickling.binary._
import scala.pickling.Defaults._

/**
  * Created by zhenghu on 16-3-12.
  */
object ReverseProxy {

  trait Msg
  case class ClientId(host: String) extends Msg
  class ClientsManager extends Actor with ActorLogging {
    override def receive: Actor.Receive = {
      case Received(data) =>
        val msg = BinaryPickle(data.iterator.asInputStream).unpickle[Msg]
        msg match {
          case ClientId(host) =>
        }

    }
  }

  class ServerController(host: String, port: Int) extends Actor with ActorLogging {
    implicit val as = context.system

    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(host, port))
    val clientManager = context.actorOf(Props(classOf[ClientsManager]), "cm")
    var addr2Server = Map[String, ActorRef]()

    override def receive: Actor.Receive = {

      case b@Bound(localAddress) =>
        log.info(s"servercontroller bound to ${localAddress}")
      case CommandFailed(bf: Bind) =>
        log.error(s"bound failed ${bf}")
        context stop self

      case c@Connected(remote, local) =>
        sender() ! Register(clientManager)

      case pm @ PortMap(rh, rp, lh, lp) =>
        val serverKey = lh+lp
        clientManager ! pm
      case msg =>
        log.warning(s"unkown message ${msg}")
    }
  }

  class Server(host: String, port: Int) extends Actor with ActorLogging {
    implicit val as = context.system
    IO(Tcp) ! Tcp.Bind(self, new InetSocketAddress(host, port))

    override def receive: Receive = {
      case b@Bound(localAddress) =>
        log.info(s"server bound to ${localAddress}")
      case CommandFailed(bf: Bind) =>
        log.error(s"bound failed ${bf}")
        context stop self
      case c@Connected(remote, local) =>
        val handler = context.actorOf(Props(classOf[FrontConnection], sender()), "front_" + System.currentTimeMillis())
        log.info(s"create a new connection ${handler.path} from ${remote}")
        log.info(s"current connection count: ${context.children.size}")
        val connection = sender()
        connection ! Register(handler)
      case msg =>
        log.warning(s"unkown message ${msg}")
    }
  }

  class FrontConnection(client: ActorRef) extends Actor with ActorLogging {
    val addr = new InetSocketAddress("192.168.0.103", 3306)
    implicit val as = context.system
    IO(Tcp) ! Tcp.Connect(addr)

    override def receive: Actor.Receive = {
      case connected: Tcp.Connected =>
        val serverConn = context.actorOf(Props(classOf[BackConnection], sender()), "back_" + System.currentTimeMillis())
        log.debug(s"create a new connection ${self.path} to ${addr}")
        val handleData: Actor.Receive = {
          case Received(data) =>
            serverConn ! ClientData(data)
          case ServerData(data) =>
            client ! Tcp.Write(data)
          case PeerClosed =>
            context.stop(serverConn)
            context.stop(self)
            log.info(s"stop connection ${self}")
        }
        context.become(handleData)
    }
  }

  class BackConnection(server: ActorRef) extends Actor with ActorLogging {

    server ! Tcp.Register(self)

    override def receive: Actor.Receive = {
      case Received(data) =>
        context.parent ! ServerData(data)
      case ClientData(data) =>
        server ! Tcp.Write(data)
    }
  }

  class Client extends Actor with ActorLogging {
    if (self.path.toString != "akka://rp_server/user/server") {
      log.info(s"my path: ${self.path}")
      val selection =
        context.actorSelection("//rp_server/user/server")
      //selection ! "hi"
      val remoteController = Await.result(selection.resolveOne(100 seconds), 100 seconds)
      remoteController ! "hi"
    }

    override def receive: Actor.Receive = {
      case e =>
        log.info(s"${self.path} recieve: ${e}")
    }
  }

  case class ClientData(data: ByteString)

  case class ServerData(data: ByteString)

  case class PortMap(remoteHost: String, remotePort: Int, localHost: String, localPort: Int)

  def startServer(config: Config): Unit = {
    val as = ActorSystem("rp_server", config)
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

  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    args(0) match {
      case "server" =>
        startServer(config.getConfig("wannagent.reverseproxy.server").withFallback(config.getConfig("wannagent.reverseproxy")).withFallback(ConfigFactory.defaultReference()))

      case "client" =>
        val as = ActorSystem("rp_server", config.getConfig("wannagent.reverseproxy.client").withFallback(ConfigFactory.defaultReference()))
        as.actorOf(Props(classOf[Client]), "client")
      case _ =>
    }
  }
}
