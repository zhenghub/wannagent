package spray.can.parsing

import akka.util.ByteString
import com.typesafe.config.Config
import org.freefeeling.wannagent.http.HttpRequestWithOrigin
import spray.can.parsing.Result.Emit
import spray.can.server.ServerSettings
import spray.http.HttpRequest

/**
  * Created by zh on 15-12-14.
  */
class HttpRequestParser(_settings: ParserSettings, rawRequestUriHeader: Boolean = false)
extends HttpRequestPartParser(_settings, rawRequestUriHeader)(HttpHeaderParser(_settings)){

  def parseRequest(data: ByteString) = {
    parseMessage(data, 0) match {
      case Emit(part: HttpRequest, _, _) =>
        HttpRequestWithOrigin(data, part)
    }
  }
}

object HttpRequestParser {
  def apply(config: Config) = {
    val serverSettings = ServerSettings.fromSubConfig(config.getConfig("spray.can.server"))
    new HttpRequestParser(serverSettings.parserSettings, serverSettings.rawRequestUriHeader)
  }
}
