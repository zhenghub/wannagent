package spray.can

import java.net.InetSocketAddress

import akka.event.NoLogging
import com.typesafe.config.Config
import spray.can.client.ClientConnectionSettings
import spray.can.rendering.{RequestRenderingComponent, RequestPartRenderingContext}
import spray.http.HttpHeaders.`User-Agent`
import spray.http.{HttpRequest, HttpDataRendering}

/**
  * Created by zh on 15-12-20.
  */
class HttpRequestRender(requestHeaderSizeHint: Int) extends RequestRenderingComponent {

  def renderRequest(request: HttpRequest, remote: InetSocketAddress) = {
    val ctx = RequestPartRenderingContext(request, request.protocol)
    val rendering = new HttpDataRendering(requestHeaderSizeHint)
    renderRequestPartRenderingContext(rendering, ctx, remote, NoLogging)
    rendering.get.toByteString
  }

  override def userAgent: Option[`User-Agent`] = None

  override def chunklessStreaming: Boolean = ???
}

object HttpRequestRender {

  def apply(config: Config) = {
    val clientSettings = ClientConnectionSettings.fromSubConfig(config.getConfig("spray.can.client"))
    new HttpRequestRender(clientSettings.requestHeaderSizeHint)
  }
}