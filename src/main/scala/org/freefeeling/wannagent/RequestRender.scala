package org.freefeeling.wannagent

import java.net.InetSocketAddress

import com.typesafe.config.Config
import spray.can.HttpRequestRender
import spray.http.HttpRequest

/**
  * Created by zh on 15-12-27.
  */
class RequestRender(config: Config) {

  def renderRequest(request: HttpRequest, remote: InetSocketAddress) = HttpRequestRender(config).renderRequest(request, remote)

}

object RequestRender {
  def apply(config: Config) = {
    new RequestRender(config)
  }
}
