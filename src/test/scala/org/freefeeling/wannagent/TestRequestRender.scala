package org.freefeeling.wannagent

import akka.util.ByteString
import org.freefeeling.wannagent.http.HttpProxyProtocol.RequestRender
import org.freefeeling.wannagent.http.RequestParser
import org.scalatest.FlatSpec
import MockRequest._

/**
  * Created by zh on 17-3-9.
  */
class TestRequestRender extends FlatSpec{

  "RequestRender" should "remove the host in the url" in {
    val anquan = """GET http://static.anquan.org/static/outer/js/aq_auth.js HTTP/1.1
                   |Host: static.anquan.org
                   |Proxy-Connection: keep-alive
                   |Cache-Control: max-age=0
                   |User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36
                   |Accept: */*
                   |Referer: http://www.acfun.cn/v/ac3528835
                   |Accept-Encoding: gzip, deflate, sdch
                   |Accept-Language: zh-CN,zh;q=0.8
                   |Cookie: __jsluid=93d26be3512fcddaeb5523cd15636ac4""".stripMargin

    val rp = RequestParser.parseRequest(mockRequest(anquan))

    val res = RequestRender.renderRequest(rp.request).utf8String
    println(res)

    assertResult(readRequest("""GET /static/outer/js/aq_auth.js HTTP/1.1
                               |Host: static.anquan.org
                               |Connection: keep-alive
                               |Cache-Control: max-age=0
                               |User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36
                               |Accept: */*
                               |Referer: http://www.acfun.cn/v/ac3528835
                               |Accept-Encoding: gzip, deflate, sdch
                               |Accept-Language: zh-CN,zh;q=0.8
                               |Cookie: __jsluid=93d26be3512fcddaeb5523cd15636ac4""".stripMargin))(res)
  }

}
