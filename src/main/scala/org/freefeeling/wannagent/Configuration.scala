package org.freefeeling.wannagent

import com.typesafe.config.ConfigFactory

/**
  * Created by zhenghu on 16-8-30.
  */
object Configuration {

  def defaultConfig() = {
    ConfigFactory.load(this.getClass.getClassLoader, "application.conf").getConfig("wannagent")
  }

}
