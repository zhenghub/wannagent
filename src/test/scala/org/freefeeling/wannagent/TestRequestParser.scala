package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.util.ByteString
import org.scalatest.WordSpec
import spray.http.Uri

/**
  * Created by zh on 15-12-27.
  */
class TestRequestParser extends WordSpec {

  def request1 =
    """CONNECT tiles.services.mozilla.com:443 HTTP/1.1
      |User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:43.0) Gecko/20100101 Firefox/43.0
      |Proxy-Connection: keep-alive
      |Connection: keep-alive
      |Host: tiles.services.mozilla.com:443""".stripMargin

  def connectRequest =
    """|CONNECT 0.gravatar.com:443 HTTP/1.1""".stripMargin

  def readRequest(request: String) = request.lines.mkString("\r\n") + "\r\n\r\n"

  def mockRequest(testExample: String) = ByteString(readRequest(testExample))

  "A request parser" when {
    "parse a common request" should {
      "recognize the host" in {
        val originBytes = mockRequest(request1)
        val parsedRequest = new RequestParser(null).parseRequest(originBytes)
        assert(parsedRequest.host == new InetSocketAddress("tiles.services.mozilla.com", 443))
        val addrUri = Uri("tiles.services.mozilla.com:443")
        val portUri = Uri("http://something.com:80")
        val absoUri = Uri("/path/to/sth")
        val relUri = Uri("path/to/sth")
        val addrUri2 = Uri("w.gravatar.com:443")
      }
    }

    "parse a connect request" should {
      "handle the uri" in {
        val bytes = mockRequest(connectRequest)
        val request = new RequestParser(null).parseRequest(bytes)
        assert(request.request.uri.toRelative == Uri./)
      }
    }
  }

}
