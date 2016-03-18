package org.freefeeling.wannagent.distributed

import akka.util.ByteString

import scala.pickling.shareNothing._
import scala.pickling.static._
import scala.pickling.binary._
import scala.pickling.Defaults._
/**
  * Created by zhenghu on 16-3-17.
  */
trait Msg {
}

object Msgs {

  def apply(bs: ByteString) = {
    BinaryPickle(bs.iterator.asInputStream).unpickle[Msg]
  }

  implicit def pickle2ByteString(pickle: BinaryPickle) = {
    ByteString(pickle.value)
  }

  case class ClientId(host: String) extends Msg
  case class ClientConnect(port: Int) extends Msg

  case class Tunnel(hostId: String, clientHost: String, clientPort: Int, id: Long) extends Msg

}
