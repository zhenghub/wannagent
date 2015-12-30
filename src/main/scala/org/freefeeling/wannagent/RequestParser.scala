package org.freefeeling.wannagent

import akka.util.ByteString
import com.typesafe.config.Config
import spray.http.HttpHeaders.RawHeader
import spray.http._

import scala.annotation.tailrec

/**
  * Created by zh on 15-12-20.
  */
class RequestParser(config: Config) {
//  val parser = HttpRequestParser(config)

  def parseRequest(coded: ByteString) = {
//    parser.parseRequest(coded)
    val res = new StateRequestParser(coded)
    res.parseRequest()
    HttpRequestWithOrigin(coded, HttpRequest(res.method, res.uri, res.headers, res.entity, res.protocol))
  }
}

object StateRequestParser extends Enumeration {
  val INIT, R1, N1, R2, END = Value

  def trans(status: Value, ch: Byte) = {
    ch match {
      case '\r' =>
        status match {
          case INIT => R1
          case N1 => R2
          case _ => INIT
        }
      case '\n' =>
        status match {
          case R1 => N1
          case R2 => END
          case _ => INIT
        }
      case _ =>
        INIT
    }
  }
}

class StateRequestParser(coded: ByteString) {

  import StateRequestParser._

  var status = INIT
  var method: HttpMethod = _
  var uri: Uri = _
  var protocol: HttpProtocol = _
  var headers: List[HttpHeader] = Nil
  var entity: HttpEntity = _

  trait LineParser {

    def origin: ByteString

    def endOfLine(lastIdx: Int)

  }

  private class MethodLineParser(val start: Int, val origin: ByteString) extends LineParser {

    var propIdx = 0
    var lastSep: Int = start - 1

    def processSep(idx: Int): Unit = {
      if (lastSep + 1 == idx) lastSep = idx // continuing separations
      else {
        val prop = origin.slice(lastSep + 1, idx).utf8String
        propIdx match {
          case 0 =>
            method = HttpMethods.getForKey(prop.toUpperCase()).get
          case 1 =>
            uri = Uri.from(path = if(method == HttpMethods.CONNECT) "/" else prop)
          case 2 =>
            protocol = HttpProtocols.getForKey(prop.toUpperCase).get
          case _ =>
        }
        propIdx += 1
        lastSep = idx
      }
    }

    override def endOfLine(lastIdx: Int): Unit = {
      val lineEnd = lastIdx - 1
      @tailrec def parse(coded: ByteString, idx: Int): Unit = {
        idx match {
          case _ if idx == lineEnd =>
            processSep(idx)
            return
          case _ =>
            if (coded(idx) == ' ') {
              processSep(idx)
            }
            parse(coded, idx + 1)
        }
      }
      parse(origin, start)
    }

  }

  class PropLineParser(val start: Int, val origin: ByteString) extends LineParser {

    private def trim(coded: ByteString, start: Int, end: Int): String = {
      var s = start
      var e = end - 1
      while (coded(s) == ' ') s += 1
      while (coded(e) == ' ') e -= 1
      coded.slice(s, e + 1).utf8String
    }

    override def endOfLine(lastIdx: Int): Unit = {
      val lineEnd = lastIdx - 1
      @tailrec def parse(coded: ByteString, idx: Int): Unit = {
        coded(idx) match {
          case ':' =>
            headers = RawHeader(trim(coded, start, idx), trim(coded, idx + 1, lineEnd)) :: headers
            return
          case _ =>
            parse(coded, idx + 1)
        }
      }
      parse(origin, start)
    }
  }

  def parseRequest(): Unit = {
    @tailrec def parse(coded: ByteString, idx: Int, lineParser: LineParser): Unit = {
      status = trans(status, coded(idx))
      status match {
        case END =>
          entity = HttpEntity(if(coded.size > idx + 1) coded.drop(idx + 1).toArray else new Array[Byte](0))
          return
        case N1 =>
          lineParser.endOfLine(idx)
          parse(coded, idx + 1, new PropLineParser(idx + 1, coded))
        case _ =>
          parse(coded, idx + 1, lineParser)
      }
    }

    parse(coded, 0, new MethodLineParser(0, coded))
  }

}
