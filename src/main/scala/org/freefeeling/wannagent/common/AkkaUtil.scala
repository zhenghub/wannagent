package org.freefeeling.wannagent.common

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern._

import scala.concurrent.Await

/**
  * Created by zhenghu on 16-3-17.
  */
object AkkaUtil {
  import scala.concurrent.duration._

  val queryComponentTimeOut = Timeout(5 seconds)

  def duration(seconds: Long) = if (seconds == 5) queryComponentTimeOut else Timeout(seconds.seconds)

  def waitRes[T](ref: ActorRef, cmd: Any, seconds: Long = 5) = {
    try {
      implicit val timeout = duration(seconds)
      val future = ref ? cmd
      Await.result(future, timeout.duration).asInstanceOf[T]
    } catch {
      case e: java.util.concurrent.TimeoutException =>
        throw new RuntimeException(s"timeout for wait for result of ${ref} executing ${cmd}", e)
    }
  }
}
