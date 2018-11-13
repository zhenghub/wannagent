package org.freefeeling.wannagent.modules

import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongBinaryOperator
import java.util.{Date, Timer, TimerTask}

import akka.stream.scaladsl.BidiFlow
import akka.util.ByteString

object ThroughputLogger {

  /**
    * record length of bytes sent and received
    * @return
    */
  def apply() = {
    val sendSize = new AtomicLong(0)
    val recvSize = new AtomicLong(0)
    val plusAtomic = new LongBinaryOperator {
      override def applyAsLong(left: Long, right: Long) = left + right
    }
    val timerTask = new TimerTask {
      override def run() = {
        println(s"${new Date}: send size: ${sendSize.get()}; recv size: ${recvSize.get()}")
      }
    }
    val timer = new Timer(true)
    timer.schedule(timerTask, 2000, 10000)

    BidiFlow.fromFunctions(
      (outgoing: ByteString) => {
        sendSize.accumulateAndGet(outgoing.size, plusAtomic);
        outgoing
      },
      (income: ByteString) => {
        recvSize.accumulateAndGet(income.size, plusAtomic);
        income
      }
    )

  }
}
