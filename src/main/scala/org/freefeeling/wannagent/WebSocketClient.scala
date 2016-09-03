package org.freefeeling.wannagent

import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import akka.util.ByteString
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import com.typesafe.sslconfig.ssl.SSLConfigFactory

import scala.concurrent.Future
import scala.collection.convert.wrapAsScala._

/**
  * Created by zh on 16-8-25.
  */
object WebSocketClient {

  def main(args: Array[String]) {
    val config = Configuration.defaultConfig().getConfig("websocketclient")
    config.getConfigList("servers").foreach { server =>
      val port = server.getInt("port")
      val address = server.getString("redirectTo")

      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      import system.dispatcher

      val connections: Source[IncomingConnection, Future[ServerBinding]] =
        akka.stream.scaladsl.Tcp().bind("0.0.0.0", port)
      val ext = Http()

      val sslCtxt = if (server.hasPath("outconnection.enablessl") && server.getBoolean("outconnection.enablessl"))
        Some(ext.createClientHttpsContext(AkkaSSLConfig().withSettings(SSLConfigFactory.parse(server.getConfig("outconnection.ssl-config.ssl").withFallback(system.settings.config.getConfig("ssl-config"))))))
      else None

      connections runForeach { connection =>
        // the Future[Done] is the materialized value of Sink.foreach
        // and it is completed when the stream completes
        val flow = Flow[Message].flatMapConcat { msg =>
          msg match {
            case BinaryMessage.Strict(data) =>
              Source.single(data)
            case TextMessage.Strict(text) =>
              Source.single(ByteString(text))
            case BinaryMessage.Streamed(src) =>
              src
            case TextMessage.Streamed(src) =>
              src.map(ByteString(_))
          }
        }.via(connection.flow).map(BinaryMessage.Strict)

        // upgradeResponse is a Future[WebSocketUpgradeResponse] that
        // completes or fails when the connection succeeds or fails
        // and closed is a Future[Done] representing the stream completion from above

        val (upgradeResponse, closed) = {
          sslCtxt match {
            case Some(connCtxt) =>
              ext.singleWebSocketRequest(WebSocketRequest(s"ws://${address}"), flow, connectionContext = connCtxt)
            case None =>
              ext.singleWebSocketRequest(WebSocketRequest(s"ws://${address}"), flow)
          }
        }

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

}
