package org.freefeeling.wannagent

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.scalatest.FlatSpec
import org.freefeeling.wannagent.common.Futures._
import HttpProxy._
import MockRequest._
import ProxyComponent._
import org.freefeeling.wannagent.http.HttpProxyProtocol.RequestRender
import org.freefeeling.wannagent.http.RequestParser

/**
  * Created by zh on 17-3-8.
  */
class TestProxyGraph extends FlatSpec{

  implicit val as = ActorSystem("proxy")
  implicit val am = ActorMaterializer()

  implicit def seqString2Source(seq: Seq[String]) = Source.fromIterator(() => seq.map(ByteString(_)).iterator)
  "A recognizer" should "recognize a connect" in {

    val res = Seq("GET", "POST").via(connectRecognizer).toMat(Sink.seq)(Keep.right).run().sync
    res.foreach(println)

  }

  "A seqMatcher" should "match target" in {
    val m = seqMatcher("abc")
    assertResult(true)(m(ByteString("abc")))
    assertResult(true)(m(ByteString("abcd")))
  }

  it should "not match target" in {
    val m = seqMatcher("abc")
    assertResult(false)(m(ByteString("ab")))
    assertResult(false)(m(ByteString("abd")))
  }


  "A proxy" should "get reponse" in {
    val bytes = """GET http://www.lihaoyi.com/Ammonite/META-INF/resources/webjars/font-awesome/4.7.0/fonts/fontawesome-webfont.woff2?v=4.7.0 HTTP/1.1
    |Host: www.lihaoyi.com
    |Proxy-Connection: keep-alive
    |Cache-Control: max-age=0
    |Origin: http://www.lihaoyi.com
    |User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36
    |Accept: */*
    |Referer: http://www.lihaoyi.com/Ammonite/META-INF/resources/webjars/font-awesome/4.7.0/css/font-awesome.min.css
    |Accept-Encoding: gzip, deflate, sdch
    |Accept-Language: zh-CN,zh;q=0.8
    |Cookie: _ga=GA1.2.1559067883.1479226218""".stripMargin

    val res = Source.single(mockRequest(bytes)).via(proxyFlow).toMat(Sink.seq)(Keep.right).run().sync
    assert(res.length > 0)

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

    val res2 = Source.single(mockRequest(anquan)).via(httpRequestGroupFlow).flatMapConcat(_._2).toMat(Sink.seq)(Keep.right).run().sync
    assertResult(readRequest("""GET /static/outer/js/aq_auth.js HTTP/1.1
                   |Host: static.anquan.org
                   |Connection: keep-alive
                   |Cache-Control: max-age=0
                   |User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36
                   |Accept: */*
                   |Referer: http://www.acfun.cn/v/ac3528835
                   |Accept-Encoding: gzip, deflate, sdch
                   |Accept-Language: zh-CN,zh;q=0.8
                   |Cookie: __jsluid=93d26be3512fcddaeb5523cd15636ac4""".stripMargin))(res2.map(_.utf8String).mkString("\r\n"))
    res2.foreach(r => println(r.utf8String))
    assert(res2.length > 0)
    }

}