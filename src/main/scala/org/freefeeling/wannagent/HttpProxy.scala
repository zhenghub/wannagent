package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source}
import akka.util.ByteString
import org.freefeeling.wannagent.http.HttpProxyProtocol.RequestRender
import org.freefeeling.wannagent.http.{HttpProxyProtocol, HttpRequestWithOrigin, RequestParser}
import org.freefeeling.wannagent.modules.ThroughputLogger

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.immutable.Seq
import scala.concurrent.Future

/**
  * Created by zh on 16-8-31.
  */
object HttpProxy {


  def outgoingFlow(address: InetSocketAddress)(implicit system: ActorSystem, materializer: Materializer) = akka.stream.scaladsl.Tcp().outgoingConnection(address.getHostName, address.getPort)

  /**
    * 返回判断一个ByteString是否以str为前缀的函数
    *
    * return a function that judge wheather a ByteString starts with {str}
    *
    * @param str
    * @return
    */
  def seqMatcher(str: String) = {
    val bs = ByteString(str)
    def isHead(target: ByteString): Boolean = {
      if (bs.length > target.length)
        false
      else {
        @tailrec
        def m(pos: Int): Boolean = {
          if(pos < bs.length)
            bs(pos) == target(pos) && m(pos + 1)
          else true
        }
        m(0)
      }
    }
    isHead _
  }

  def connectProxyComponent: BidiFlow[ByteString, (InetSocketAddress, Source[ByteString, _]), Source[ByteString, _], ByteString, _] = {
    val sendRemote: Flow[ByteString,(InetSocketAddress, Source[ByteString, NotUsed]), NotUsed] = Flow[ByteString].prefixAndTail(1).map{case (Seq(first), tail) =>
      RequestParser.parseRequest(first).host -> tail
    }

    val receiveRemote: Flow[Source[ByteString, _], ByteString, _] = Flow[Source[ByteString, _]].flatMapConcat(_.prepend(Source.single(HttpProxyProtocol.`HTTP/1.0Response`)))
    BidiFlow.fromFlows(sendRemote, receiveRemote)
  }

  trait HttpComponent {
    type Req = Either[InetSocketAddress, Source[ByteString, _]]


    val httpRequestFlow: Flow[ByteString, (HttpRequestWithOrigin, Source[ByteString, NotUsed]), NotUsed] = Flow[ByteString].
      splitWhen(p => p(0) < '0' || p(0) > '9').
      prefixAndTail(1).
      map { case (Seq(first), tail) =>
        (RequestParser.parseRequest(first), tail)
      }.concatSubstreams

    val newRemoteReqFlow = {
      val addressOrDataFlow: Flow[ByteString, Req, NotUsed] = httpRequestFlow.statefulMapConcat(() => {
        var lastMsg: Option[HttpRequestWithOrigin] = None
        (ele) => {
          ele match {
            case (msg, tail) =>
              val req = Right(tail.prepend(Source.single(RequestRender.renderRequest(msg.request))))
              lastMsg.map(m => m.host == msg.host).getOrElse(false) match {
                case true =>
                  immutable.Seq(req)
                case false =>
                  immutable.Seq(Left(msg.host), req)
              }

          }

        }
      })

      val subFlow = Flow[Req].splitWhen(_.isLeft).prefixAndTail(1).map { case (Seq(Left(address)), tail) =>
        address -> (tail.flatMapConcat(_.right.get): Source[ByteString, _])
      }
      val reqGroupFlow: Flow[Req, (InetSocketAddress, Source[ByteString, _]), _] = subFlow
        .concatSubstreams

      addressOrDataFlow.via(reqGroupFlow)
    }

    //      address -> tail.map(_.right.get).map(req => req._2.prepend(Source.single(req._1.origin)))//.via(outgoingFlow(address))
    //    }.concatSubstreams

    val httpRequestGroupFlow: Flow[ByteString, (InetSocketAddress, Source[ByteString, _]), _] = newRemoteReqFlow
    // val sendRemoteFlow: Flow[Source[(InetSocketAddress, Source[ByteString, _]), _], ByteString, _] = ???
    val remoteResponseFlow: Flow[Source[ByteString, _], ByteString, _] = Flow[Source[ByteString, _]].flatMapConcat(x => x)

    val httpProxyComponent: BidiFlow[ByteString, (InetSocketAddress, Source[ByteString, _]), Source[ByteString, _], ByteString, _] = BidiFlow.fromFlows(httpRequestGroupFlow, remoteResponseFlow)
  }

  object HttpComponent extends HttpComponent

  trait ProxyComponent extends HttpComponent{
    val isConnect = seqMatcher("CONNECT")

    // left means simple connect while right means parse every request and redirect to different target according to the request
    type CONNECT_OTHER = Either[ByteString, ByteString]

    val connectStateHandler:() => (ByteString) => immutable.Iterable[CONNECT_OTHER] = () => {
      var connected = false
      (p) => {
        if (!connected && isConnect(p)) {
          connected = true
          immutable.Seq(Left(p))
        } else {
          immutable.Seq(Right(p))
        }
      }
    }

    val connectRecognizer = Flow[ByteString].statefulMapConcat[CONNECT_OTHER](connectStateHandler)
  }

  object ProxyComponent extends ProxyComponent

  def proxy(outgoingFlow: Flow[(InetSocketAddress, Source[ByteString, _]), Source[ByteString, _], _]): Flow[ByteString, ByteString, _] = {
    import ProxyComponent._

    val graph: Flow[ByteString, ByteString, _] = connectRecognizer.splitWhen(p =>
      p.isLeft
    ).prefixAndTail(1).flatMapConcat { case (Seq(first), tail) =>
      val f: Source[ByteString, NotUsed] = first match {
        case Right(http) =>
          tail.map(_.right.get).prepend(Source.single(http)).via(httpProxyComponent.join(outgoingFlow))
        case Left(connect) =>
          tail.map(_.right.get).prepend(Source.single(connect)).via(connectProxyComponent.join(outgoingFlow))
      };
      f
    }.concatSubstreams

    graph
  }

  def outgoingConnectionFlow(implicit as: ActorSystem, am: ActorMaterializer) = Flow[(InetSocketAddress, Source[ByteString, _])].map { case (addr, data) =>
    data.via(outgoingFlow(addr))
  }

  def proxyFlow(implicit as: ActorSystem, am: ActorMaterializer) = proxy(outgoingConnectionFlow)

  def runProxy(listenAddress: InetSocketAddress) = {
    implicit val as = ActorSystem("http-proxy")
    implicit val am = ActorMaterializer()

    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      akka.stream.scaladsl.Tcp().bind(listenAddress.getHostName, listenAddress.getPort)
    val outgoing = ThroughputLogger.apply().join(proxy(outgoingConnectionFlow))
    val serverGraph = connections.to(Sink.foreach(connection =>   connection.handleWith(outgoing)))
    val binding = serverGraph.run()

  }

}
