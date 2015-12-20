package org.freefeeling.wannagent

import akka.util.ByteString
import com.typesafe.config.Config
import spray.can.parsing.HttpRequestParser

/**
  * Created by zh on 15-12-20.
  */
class RequestParser(config: Config) {
  val parser = HttpRequestParser(config)

  def parseRequest(coded: ByteString) = {
    parser.parseRequest(coded)
  }
}
