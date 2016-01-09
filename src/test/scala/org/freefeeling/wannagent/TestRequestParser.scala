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

  def noPortHttpsConnRequest =
  """CONNECT www.baidu.com:443 HTTP/1.1
    |Host: www.baidu.com
    |Proxy-Connection: keep-alive
    |User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 Safari/537.36 SE 2.X MetaSr 1.0""".stripMargin

  def youku = """GET http://youku.com/ HTTP/1.1
                |Host: youku.com
                |User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:43.0) Gecko/20100101 Firefox/43.0
                |Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
                |Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3
                |Accept-Encoding: gzip, deflate
                |Connection: keep-alive
                |Pragma: no-cache
                |Cache-Control: no-cache""".stripMargin

  def youkuPlay = """GET http://yk.pp.navi.youku.com/playpolicy/get.json?vid_encode=XMTQzOTQzMzM2MA==&category=85&caller=PLAYER HTTP/1.1
                    |Host: yk.pp.navi.youku.com
                    |Proxy-Connection: keep-alive
                    |User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36
                    |X-Requested-With: ShockwaveFlash/20.0.0.228
                    |Accept: */*
                    |Referer: http://v.youku.com/v_show/id_XMTQzOTQzMzM2MA==.html?f=26496693&ev=1
                    |Accept-Encoding: gzip, deflate, sdch
                    |Accept-Language: zh-CN,zh;q=0.8
                    |Cookie: __ysuid=1452303949866EJK; isAppearLoginGuide=true; ykss=53669056839c7a1013edb30b; __ali=1452303959237l4t; advideo206014_2=1; advideo206025_2=1; advideo206026_2=2; __aliCount=1; xreferrer=http://www.youku.com/; premium_cps_vid=XMTQzOTQzMzM2MA%3D%3D; u=__LOGOUT__""".stripMargin

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

        new RequestParser(null).parseRequest(mockRequest(youkuPlay))
      }
      "get the host in connect line" in {
        val bytes = mockRequest(noPortHttpsConnRequest)
        val request = new RequestParser(null).parseRequest(bytes)
        assert(request.host == new InetSocketAddress("www.baidu.com", 443))

        assert(new RequestParser(null).parseRequest(mockRequest(youku)).host == new InetSocketAddress("youku.com", 80))
      }
    }
  }

}
