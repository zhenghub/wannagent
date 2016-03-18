package org.freefeeling.wannagent

import org.freefeeling.wannagent.common.TreeCli

/**
  * Created by zhenghu on 16-3-18.
  */
object Wannagent {

  val cli = new TreeCli( Map(
    "proxyserver" -> ProxyServer.main _,
    "reverse" -> ReverseProxy.main _
  ))

  def main(args: Array[String]): Unit = {
    cli.resolve(args)
  }
}
