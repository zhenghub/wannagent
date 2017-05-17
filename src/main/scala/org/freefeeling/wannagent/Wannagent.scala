package org.freefeeling.wannagent

import org.backuity.clist.Command

/**
  * Created by zhenghu on 16-3-18.
  */
object Wannagent {
  import org.backuity.clist._
  import org.freefeeling.wannagent.common.IFreeString._

  object HttpProxy extends CliMain[Unit](description = "http proxy") {
    var listenAddr = arg[String](name = "listenAddress", description = "监听地址")

    def run: Unit = {
      ProxyServerOnStream.runProxy(listenAddr.toInetSocketAddress)
    }
  }

  object DirectProxy extends CliMain[Unit](description = "直接转发") {
    var listenAddr = arg[String](name = "listenAddress", description = "监听地址")
    var forwardAddr = arg[String](name = "forwardAddress", description = "转发地址")

    def run: Unit = {
      ForwardProxy.apply(ForwardProxy.Param(listenAddr.toInetSocketAddress, forwardAddr.toInetSocketAddress))
    }
  }

  def main(args: Array[String]): Unit = {
    Cli.parse(args).withProgramName("wannagent").withCommands[CliMain[Unit]](HttpProxy, DirectProxy).foreach(_.run)
  }
}
