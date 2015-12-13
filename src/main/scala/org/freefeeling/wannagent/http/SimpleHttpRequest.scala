package org.freefeeling.wannagent.http

import akka.util.ByteString
import spray.http._

/**
  * Created by zh on 15-12-13.
  */
case class SimpleHttpRequest(val origin: ByteString, override val method: HttpMethod = HttpMethods.GET,
                        override val uri: Uri = Uri./,
                        override val headers: List[spray.http.HttpHeader] = Nil,
                        override val entity: HttpEntity = HttpEntity.Empty,
                        override val protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`)
  extends HttpRequest(method, uri, headers, entity, protocol) {


}
