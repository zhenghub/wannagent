package org.freefeeling.wannagent.http

import akka.util.ByteString
import spray.http._

/**
  * Created by zh on 15-12-13.
  */
case class HttpRequestWithOrigin(val origin: ByteString, val request: HttpRequest)
