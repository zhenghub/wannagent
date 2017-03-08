package org.freefeeling.wannagent.common

/**
  * Created by zh on 17-2-26.
  */
object Exceptions {

  class RequestException(msg: String) extends RuntimeException(msg)
  def reqeustException(msg: String) = throw new RequestException(msg)

}
