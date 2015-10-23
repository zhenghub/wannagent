package org.freefeeling.wannagent.common

import org.freefeeling.wannagent.common.TreeCli.Action

/**
  * Created by zhenghu on 16-3-18.
  */
class TreeCli(cmd2Action: Map[String, Action]) {

  def resolve(args: Array[String]): Unit = {
    val cmd = if(args.size == 0) "" else args(0)
    cmd2Action.get(cmd) match {
      case Some(action) => action(args.drop(1))
      case None =>
        println(s"""usage: ${cmd2Action.keys.mkString(", ")}""")
    }
  }

}

object TreeCli {
  type Action = Array[String] => Unit
  implicit def lambda2Action[T](l: => T) = {
    (args: Array[String]) => l
  }
}
