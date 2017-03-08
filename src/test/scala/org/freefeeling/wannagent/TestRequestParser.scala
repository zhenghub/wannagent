package org.freefeeling.wannagent

import java.net.InetSocketAddress

import MockRequest._
import org.freefeeling.wannagent.http.{RequestParser, Uri}
import org.scalatest.WordSpec

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

  def youku =
    """GET http://youku.com/ HTTP/1.1
      |Host: youku.com
      |User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:43.0) Gecko/20100101 Firefox/43.0
      |Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
      |Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3
      |Accept-Encoding: gzip, deflate
      |Connection: keep-alive
      |Pragma: no-cache
      |Cache-Control: no-cache""".stripMargin

  def youkuPlay =
    """GET http://yk.pp.navi.youku.com/playpolicy/get.json?vid_encode=XMTQzOTQzMzM2MA==&category=85&caller=PLAYER HTTP/1.1
      |Host: yk.pp.navi.youku.com
      |Proxy-Connection: keep-alive
      |User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36
      |X-Requested-With: ShockwaveFlash/20.0.0.228
      |Accept: */*
      |Referer: http://v.youku.com/v_show/id_XMTQzOTQzMzM2MA==.html?f=26496693&ev=1
      |Accept-Encoding: gzip, deflate, sdch
      |Accept-Language: zh-CN,zh;q=0.8
      |Cookie: __ysuid=1452303949866EJK; isAppearLoginGuide=true; ykss=53669056839c7a1013edb30b; __ali=1452303959237l4t; advideo206014_2=1; advideo206025_2=1; advideo206026_2=2; __aliCount=1; xreferrer=http://www.youku.com/; premium_cps_vid=XMTQzOTQzMzM2MA%3D%3D; u=__LOGOUT__""".stripMargin

  def youkuPost =
    """POST http://e.stat.youku.com/vt/vt HTTP/1.1
      |Host: e.stat.youku.com
      |Proxy-Connection: keep-alive
      |Content-Length: 103
      |Origin: http://v.youku.com
      |X-Requested-With: ShockwaveFlash/20.0.0.228
      |User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36
      |Content-Type: application/x-www-form-urlencoded
      |Accept: */*
      |Referer: http://v.youku.com/v_show/id_XMTQ0MDU1Mjc2OA==.html?f=26497807&ev=1&from=y1.3-idx-uhome-1519-20887.205805-205902.1-1
      |Accept-Encoding: gzip, deflate
      |Accept-Language: zh-CN,zh;q=0.8
      |Cookie: __ysuid=1452303949866EJK; __ali=1452303959237l4t; __aliCount=1; u_l_v_t=1918; advideo206014_2=1; advideo206025_2=2; advideo206026_2=2; isAppearLoginGuide=true; ykss=292691564047b9bc1a4eec91; advideo={"adv205916_3": 2, "adv205987_3": 2}; rpabtest=A-1452353073276; __utmarea=; xreferrer=http://www.youku.com/; premium_cps_vid=XMTQ0MDU1Mjc2OA%3D%3D; r="8MaZyz7N+UYvw0SPC+D/SzXMnaMxlduh9hOeuwsu+Ki4mtkYxAlaSo3BYb4qWZ4Z0Q8JZhogtBkgZF+ydldrwIHEmeSoX8tAxI5lnQdCBN+uQXQm/sLtW7O02JANCxsys6vHQWSXabhGWmAX7P5rGE+H97bZGT+arfXCMWef8mU="; P_F=1; view=1; _l_s_c_=a%3A0%3A%7B%7D; _l_s_c_n_=a%3A2%3A%7Bs%3A9%3A%22_l_n_s_id%22%3Bs%3A16%3A%221452353078316Cir%22%3Bs%3A11%3A%22_l_n_s_step%22%3Bi%3A1%3B%7D; _l_f_c_=a%3A5%3A%7Bs%3A8%3A%22_l_r_pid%22%3Bs%3A19%3A%221452353073720ETMFSJ%22%3Bs%3A8%3A%22_l_r_cid%22%3Bs%3A1%3A%22v%22%3Bs%3A16%3A%22_l_t_first_gself%22%3Bi%3A1452336212000%3Bs%3A12%3A%22_l_n_s_count%22%3Bi%3A2%3Bs%3A10%3A%22_l_v_gself%22%3Bi%3A5%3B%7D; u=__LOGOUT__; P_T=1452360525; rpvid=1452353073720ETMFSJ-1452353335670""".stripMargin

  def melpaGet =
    """GET https://melpa.org/packages/ HTTP/1.1
      |MIME-Version: 1.0
      |Connection: close
      |Extension: Security/Digest Security/SSL
      |Host: melpa.org
      |Accept-encoding: gzip
      |Accept: */*
      |User-Agent: URL/Emacs""".stripMargin

  def curlGet =
    """GET http://freefeeling.org/ HTTP/1.1
      |Host: freefeeling.org
      |User-Agent: curl/7.47.0
      |Accept: */*
      |Proxy-Connection: Keep-Alive""".stripMargin

  "A request parser" when {
    "parse a common request" should {
      "recognize the host" in {
        val originBytes = mockRequest(request1)
        val parsedRequest = RequestParser.parseRequest(originBytes)
        assert(parsedRequest.host == new InetSocketAddress("tiles.services.mozilla.com", 443))
        assert("tiles.services.mozilla.com:443" == Uri("tiles.services.mozilla.com:443").host.get)
        assert("something.com:80" == Uri("http://something.com:80").host.get)
        assert("/" == Uri("http://www.youku.com.cn/").path.get)
        assert("/path/to/sth" == Uri("/path/to/sth").path.get)
        assert("/to/sth" == Uri("path/to/sth").path.get)
      }
    }

    "parse a curl get request" should {
      "recognize the header" in {
        val originBytes = mockRequest(curlGet)
        val request = RequestParser.parseRequest(originBytes)
        assertResult("freefeeling.org")(request.host.getHostName)
        assertResult("http://freefeeling.org/")(request.request.uri.toString)
        assertResult("Keep-Alive")(request.request.headers.find(_.name == "Proxy-Connection").get.value)
      }
    }

    "parse a melpa request" should {
      "recognize the url" in {
        val originBytes = mockRequest(melpaGet)
        assertResult("/packages/")(RequestParser.parseRequest(originBytes).request.uri.path.get)
      }
    }

    "parse a connect request" should {
      "handle the uri" in {
        val bytes = mockRequest(connectRequest)
        val request = RequestParser.parseRequest(bytes)
        assert(request.request.uri.toRelative.path == Uri./.path)

        {
          val playUri = RequestParser.parseRequest(mockRequest(youkuPlay)).request.uri.toRelative
          assertResult("/playpolicy/get.json?vid_encode=XMTQzOTQzMzM2MA==&category=85&caller=PLAYER")(playUri.path.get + "?" + playUri.args.get)
        }
      }
      "get the host in connect line" in {
        val bytes = mockRequest(noPortHttpsConnRequest)
        val request = RequestParser.parseRequest(bytes)
        assert(request.host == new InetSocketAddress("www.baidu.com", 443))

        assert(RequestParser.parseRequest(mockRequest(youku)).host == new InetSocketAddress("youku.com", 80))
      }
    }
  }

}
