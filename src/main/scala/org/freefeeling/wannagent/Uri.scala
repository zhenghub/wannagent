package org.freefeeling.wannagent

import java.util.regex.Pattern

import scala.util.parsing.combinator.RegexParsers

/**
  * Created by zh on 16-1-9.
  */
class Uri(scheme: Option[String], host: Option[String], path: Option[String], args: Option[String]) {

  def toRelative =
    Uri(None, None, if (path.isEmpty) Option("/") else path, args)

  override def toString = s"${scheme.map(_ + "//").getOrElse("")}${host.getOrElse("")}${path.getOrElse("")}${args.map("?" + _).getOrElse("")}"
}

object Uri {

  val / = Uri(None, None, Option("/"), None)

  val scheme = """(?<sc>http)"""
  val uriPattern = Pattern.compile(s"""(${scheme}://(?<host>.*?))(?<path>/[^?]*)(\\?(?<args>.*))?""")
  class UriParser{
    def parse(text: String) = {
      val m = uriPattern.matcher(text)
      m.find() match {
        case true =>
          Uri(Option(m.group("sc")), Option(m.group("host")), Option(m.group("path")), Option(m.group("args")))
        case false =>
          throw new RuntimeException(s"error when parsing uri ${text}")
      }
    }
  }

  val parser = new UriParser

  def apply(uri: String): Uri = parser.parse(uri)

  def apply(scheme: Option[String], host: Option[String], path: Option[String], args: Option[String]):Uri = new Uri(scheme, host, path, args)
}
