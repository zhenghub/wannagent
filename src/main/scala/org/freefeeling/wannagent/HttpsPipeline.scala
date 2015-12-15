package org.freefeeling.wannagent

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive

/**
  * created according to http://tools.ietf.org/html/draft-luotonen-web-proxy-tunneling-01
  * Created by zh on 15-12-13.
  */
class HttpsPipeline extends Actor{
  // plain http connection request from client
  // try to connect the remote server with socket
  // plain http connected response to the client if succees
  // client start ssl
  // proxy send all the data from the client including encryption to the server
  // proxy send all the data from the server including encryption to the client

  // one side disconnected, send the flying data to the other side and close the connection

  // handle client piplining
  // proxy authentication
  override def receive: Receive = ???
}

object HttpsPipeline{


  def apply = Props(classOf[HttpsPipeline])
}
