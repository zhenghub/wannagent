package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.impl.fusing.FlattenMerge
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.stage.{GraphStageLogic, GraphStage}
import akka.util.ByteString
import spray.http.HttpMethods
import scala.collection.immutable
import scala.concurrent.Future

/**
  * Created by zh on 16-8-31.
  */
object NewProxyServer {

  case class Server(port: Int)

  def outgoingFlow(address: InetSocketAddress)(implicit system:ActorSystem, materializer: Materializer) = akka.stream.scaladsl.Tcp().outgoingConnection(address.getHostName, address.getPort)

  def outgoingHttpFlow(implicit system:ActorSystem, materializer: Materializer): Flow[HttpRequestWithOrigin, ByteString, NotUsed] = {
    type HttpProxyReq = Either[InetSocketAddress, HttpRequestWithOrigin]
    def wrapEither(msg: HttpProxyReq) = msg
    def handleRequest(origin: HttpRequestWithOrigin) = {
      Option(origin.request.uri) match {
        case Some(absoluteUri) =>
          val newRequest = origin.request.copy(
            uri = absoluteUri.toRelative
            //          headers = (origin.request.headers ::: `Cache-Control`(Seq(`max-age`(0))) :: Nil)
          )
          val render = RequestRender()
          render.renderRequest(newRequest)
        case None =>
          origin.origin
      }
    }
    val transHttpProxyReq = Flow[HttpRequestWithOrigin].statefulMapConcat[HttpProxyReq] { () =>
      var lastMsg: Option[HttpRequestWithOrigin] = None
      (msg: HttpRequestWithOrigin) => {
        lastMsg.map(m => m.host == msg.host).getOrElse(false) match {
          case true =>
            immutable.Seq(wrapEither(Right(msg)))
          case false =>
            immutable.Seq(wrapEither(Left(msg.host)), wrapEither(Right(msg)))
        }
      }
    }
    val sendRemoteFlow: Flow[HttpProxyReq, ByteString, NotUsed] = Flow[HttpProxyReq].prefixAndTail(1).map { case (head, tail) =>
      val address = head(0).left.get
      tail.map(_.right.get).map(handleRequest).via(outgoingFlow(address))
    }.flatMapConcat(x => x)

    val responseFlow = Flow[HttpProxyReq].splitWhen(_.isLeft)
      .via(sendRemoteFlow).concatSubstreams
    transHttpProxyReq.via(responseFlow)
  }

  def proxyFlow(implicit system:ActorSystem, materializer: Materializer): Flow[ByteString, ByteString, _] = {
    val requestParser = new RequestParser
    val headAF = Flow[ByteString].prefixAndTail(1).map { case (head, src) =>
      val msg = head(0)
      val req = requestParser.parseRequest(msg)
      val subF = req.method match {
        case HttpMethods.CONNECT =>
          outgoingFlow(req.host).prepend(Source.single(SecureRemoteConnection.connectedResponse.origin))
        case _ =>
          val byteString2Req = Flow[ByteString].map(requestParser.parseRequest).prepend(Source.single(req))
          byteString2Req.via(outgoingHttpFlow)
      }
      src.via(subF)
    }.flatMapConcat(x => x)
    headAF
  }

  def startServer(server: Server)(implicit system:ActorSystem, materializer: Materializer): Unit = {
    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      akka.stream.scaladsl.Tcp().bind("0.0.0.0", server.port)
    connections runForeach { connection =>
      connection.handleWith(proxyFlow)
    }
  }

  def main(args: Array[String]): Unit = {
    val config = Configuration.defaultConfig().getConfig("proxy")
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    startServer(Server(config.getInt("port")))
  }

}
