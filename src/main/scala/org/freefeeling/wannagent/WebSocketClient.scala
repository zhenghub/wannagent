package org.freefeeling.wannagent

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import akka.util.ByteString
import akka.{ Done, NotUsed }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._

import scala.concurrent.Future

/**
  * Created by zh on 16-8-25.
  */
object WebSocketClient {

  def main(args: Array[String]) {
    val port = args(0).toInt
    val address = args(1)

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      akka.stream.scaladsl.Tcp().bind("0.0.0.0", port)
    connections runForeach { connection =>
      // the Future[Done] is the materialized value of Sink.foreach
      // and it is completed when the stream completes
      val flow = Flow[Message].map{msg =>
        msg match {
          case BinaryMessage.Strict(data) => data
          case TextMessage.Strict(text) =>
            ByteString(text)
        }
      }.via(connection.flow).map(BinaryMessage.Strict)

      // upgradeResponse is a Future[WebSocketUpgradeResponse] that
      // completes or fails when the connection succeeds or fails
      // and closed is a Future[Done] representing the stream completion from above
      val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(WebSocketRequest(s"ws://${address}"), flow)

      val connected = upgradeResponse.map { upgrade =>
        // just like a regular http request we can access response status which is available via upgrade.response.status
        // status code 101 (Switching Protocols) indicates that server support WebSockets
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
          Done
        } else {
          throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
        }
      }

      // and handle errors more carefully
      connected.onComplete(println)

    }
  }

}
