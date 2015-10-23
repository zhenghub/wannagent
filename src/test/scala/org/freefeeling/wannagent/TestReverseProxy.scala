package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import akka.util.ByteString
import org.freefeeling.wannagent.TestReverseProxy.MockMysqlServer2
import org.scalatest.WordSpec

/**
  * Created by zhenghu on 16-3-17.
  */
class TestReverseProxy extends WordSpec {
  "A ReverseProxy" when {
    "receive a message" should {
      "send to the other end" in {
        val as = ActorSystem("test")
        as.actorOf(Props(classOf[MockMysqlServer2]))
        Thread.sleep(10000)
      }

    }
  }
}

object TestReverseProxy {

  class MockMysqlServer2 extends Actor with ActorLogging {
    implicit val as = context.system
    IO(akka.io.Tcp) ! Tcp.Bind(self, new InetSocketAddress(38066))
    var connectCnt = 0

    override def receive: Receive = {
      case b@Bound(localAddress) =>
        log.info(s"mock mysql server bound to ${localAddress}")
      case CommandFailed(bf: Bind) =>
        log.error(s"bound failed ${bf}")
        context stop self
      case c@Connected(remote, local) =>
        connectCnt += 1
        val handler = context.actorOf(Props(classOf[FrontConnection], sender()), "front_" + System.currentTimeMillis())
        log.info(s"create a new connection ${handler.path} from ${remote}")
        log.info(s"current connection count: ${context.children.size}")
        val connection = sender()
        connection ! Register(handler)
        context.watch(handler)
      case Terminated(actor) =>
        log.info(s"${actor} stopped, current connection count: ${context.children.size}")
      case msg =>
        log.warning(s"unkown message ${msg}")
    }
  }


  class FrontConnection(client: ActorRef) extends Actor with ActorLogging {
    val addr = new InetSocketAddress("127.0.0.1", 3306)
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

  case class ClientData(data: ByteString)

  case class ServerData(data: ByteString)


  def main(args: Array[String]) {
    val as = ActorSystem("test")
    as.actorOf(Props(classOf[MockMysqlServer2]))
  }
}
