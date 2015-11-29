package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp.Register
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress

/**
  * Created by zhenghu on 15-11-29.
  */

object TestAkkaIO {

  val addr = new InetSocketAddress("127.0.0.1", 56789)

  class SimplisticHandler extends Actor {

    import Tcp._

    def receive = {
      case Received(data) =>
        println(getClass + " receive:" + data.utf8String)
        sender() ! Write(ByteString("server_get:" + data.utf8String))
      case PeerClosed => context stop self
      case ErrorClosed(msg) =>
        println("SimplisticHandler: " + msg)
        context stop self
    }
  }

  class Server extends Actor {

    import Tcp._
    import context.system

    IO(Tcp) ! Bind(self, addr)

    def receive = {
      case b@Bound(localAddress) =>
      // do some logging or setup ...

      case CommandFailed(_: Bind) => context stop self

      case c@Connected(remote, local) =>
        println("server_connected")
        val handler = context.actorOf(Props[SimplisticHandler])
        val connection = sender()
        connection ! Register(handler)
      case msg =>
        println(msg)
    }

  }

  class Client extends Actor {

    override def preStart(): Unit = {
      println("client_preStart")
      import akka.io.{IO, Tcp}
      import context.system
      // implicitly used by IO(Tcp)

      val manager = IO(Tcp)
      manager ! Tcp.Connect(addr)
    }

    override def receive: Actor.Receive = {
      case Tcp.Connected(remote, local) =>
        println("client_receive")
        println(local)
        val connection = sender()
        connection ! Tcp.Register(self)
        connection ! Tcp.Write(ByteString("hi, akkaio, I'm " + local))
      case Tcp.Received(data) =>
        println(data.utf8String)
        context stop self
      case msg =>
        println(msg)
    }
  }

  def main(args: Array[String]): Unit = {
    akka.Main.main(Array(classOf[Server].getName))
    akka.Main.main(Array(classOf[Client].getName))
    println("done")
  }

}
