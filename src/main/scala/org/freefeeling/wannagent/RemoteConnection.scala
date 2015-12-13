package org.freefeeling.wannagent

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
  * Created by zh on 15-12-13.
  */
class RemoteConnection(remote: InetSocketAddress) extends Actor{
  override def receive: Receive = ???
}
