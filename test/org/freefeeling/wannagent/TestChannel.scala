package org.freefeeling.wannagent

import java.io.ByteArrayOutputStream
import scala.actors.Actor

/**
 * @author zh
 */
object TestChannel extends App{
    
	import Debug._
	import Main.run
	
	val th:Thread = run _
    th.setDaemon(true)
	th start
    
    def testSimpleAgent() {
        import sys.process._
        val response = new ByteArrayOutputStream()
        "curl www.baidu.com" #> response !;
        assert(new String(response.toByteArray()).length() > 0)
//        th.stop()
    }
    
    
    testSimpleAgent()
  
}