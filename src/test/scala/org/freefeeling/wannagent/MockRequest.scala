package org.freefeeling.wannagent

import akka.util.ByteString

/**
  * Created by zh on 17-3-8.
  */
object MockRequest {
  def readRequest(request: String) = request.lines.mkString("\r\n") + "\r\n\r\n"

  def mockRequest(testExample: String) = ByteString(readRequest(testExample))
}
