package org.freefeeling.wannagent.common

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by zh on 17-2-26.
  */
object Futures {

  implicit class FutureExtd[E](f: Future[E]) {
    def sync = {
      Await.result(f, Duration.Inf)
    }
  }

}
