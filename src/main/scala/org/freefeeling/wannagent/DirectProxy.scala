package org.freefeeling.wannagent

import java.net.InetSocketAddress
import java.util.{Date, Timer, TimerTask}
import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongBinaryOperator

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.util.ByteString

import scala.concurrent.Future

/**
  * Created by zh on 2017/5/17.
  */
object ForwardProxy {

  case class Param(listenAddress: InetSocketAddress, forwardAddress: InetSocketAddress)

  def addressStr2Address(addr: String) = addr.split(":") match {
    case Array(host, port) => InetSocketAddress.createUnresolved(host, port.toInt)
    case _ => throw new RuntimeException(s"cannot resolve host and port from address ${addr}")
  }
  def apply(param: Param): Unit = {
    implicit val as = ActorSystem("proxy")
    implicit val am = ActorMaterializer()

    val listenAddr = param.listenAddress
    val forwardAddr = param.forwardAddress
    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      akka.stream.scaladsl.Tcp().bind(listenAddr.getHostName, listenAddr.getPort)

    val outgointConnections = akka.stream.scaladsl.Tcp().outgoingConnection(forwardAddr.getHostName, forwardAddr.getPort)
    val recordThoughput = {
      val sendSize = new AtomicLong(0)
      val recvSize = new AtomicLong(0)
      val plusAtomic = new LongBinaryOperator {
        override def applyAsLong(left: Long, right: Long) = left + right
      }
      val timerTask = new TimerTask {
        override def run() = {
          println(s"${new Date}: send size: ${sendSize.get()}; recv size: ${recvSize.get()}")
        }
      }
      val timer = new Timer(true)
      timer.schedule(timerTask, 2000, 10000)
      Flow[ByteString].map{b =>
        sendSize.accumulateAndGet(b.size, plusAtomic);
        b
      }.via(outgointConnections).map{ b =>
        recvSize.accumulateAndGet(b.size, plusAtomic);
        b
      }
    }
    val binding = connections.runForeach(_.handleWith(recordThoughput))
  }

}
