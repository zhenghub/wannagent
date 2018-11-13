package org.freefeeling.wannagent

import org.backuity.clist.Command

/**
  * Created by zhenghu on 16-3-18.
  */
object Wannagent {
  import org.backuity.clist._
  import org.freefeeling.wannagent.common.IFreeString._

  object Http extends CliMain[Unit](description = "http proxy") {
    var listenAddr = arg[String](name = "listenAddress", description = "监听地址")

    def run: Unit = {
      HttpProxy.runProxy(listenAddr.toInetSocketAddress)
    }
  }

  object Direct extends CliMain[Unit](description = "直接转发") {
    var listenAddr = arg[String](name = "listenAddress", description = "监听地址")
    var forwardAddr = arg[String](name = "forwardAddress", description = "转发地址")
    var forwardProtocol = opt[String](name = "forwardProtocol", description = "转发协议: tls, tcp", default = "tcp")

    def run: Unit = {
      DirectProxy.apply(DirectProxy.Param(listenAddr.toInetSocketAddress, forwardAddr.toInetSocketAddress, forwardProtocol))
    }
  }

  def main(args: Array[String]): Unit = {
    Cli.parse(args).withProgramName("wannagent").withCommands[CliMain[Unit]](Http, Direct).foreach(_.run)
  }
}
