package org.freefeeling.wannagent.common

import akka.actor.{ActorLogging, ActorSystem}
import akka.event.BusLogging
import akka.event.Logging.{Debug, Info, Warning, Error}
import akka.event._
import ch.qos.logback.classic.{Logger => LbLogger}
import org.slf4j.{Logger => JLogger}
import scala.Error
import scala.reflect.macros.Context
import scala.language.experimental.macros
import Logger._

/**
  * Created by zhenghu on 16-3-18.
  */

trait Logger extends LoggingAdapter{

  override def debug(message: String) = macro debugTM

}

object Logger {

  private[this] type LogCtx = Context { type PrefixType = Logger }

  def debugTM(c: LogCtx)(message: c.Expr[String]): c.Expr[Unit] = {
    import c.universe._
    reify {
      val logger = c.prefix.splice
      println("execute macro")
      if(logger.isDebugEnabled) {
        println("debug enabled")
        println(message.splice)
      }
    }
  }

  class MBusLogging(val bus: LoggingBus, val logSource: String, val logClass: Class[_]) extends Logger {

    import Logging._

    def isErrorEnabled = bus.logLevel >= ErrorLevel
    def isWarningEnabled = bus.logLevel >= WarningLevel
    def isInfoEnabled = bus.logLevel >= InfoLevel
    def isDebugEnabled = bus.logLevel >= DebugLevel

    protected def notifyError(message: String): Unit = bus.publish(Error(logSource, logClass, message, mdc))
    protected def notifyError(cause: Throwable, message: String): Unit = bus.publish(Error(cause, logSource, logClass, message, mdc))
    protected def notifyWarning(message: String): Unit = bus.publish(Warning(logSource, logClass, message, mdc))
    protected def notifyInfo(message: String): Unit = bus.publish(Info(logSource, logClass, message, mdc))
    protected def notifyDebug(message: String): Unit = bus.publish(Debug(logSource, logClass, message, mdc))
  }

  def apply[T: LogSource](system: ActorSystem, logSource: T): LoggingAdapter = {
    val (str, clazz) = LogSource(logSource, system)
    new MBusLogging(system.eventStream, str, clazz)
  }
}
