package org.freefeeling.wannagent

import java.io.InputStream

/**
 * @author zh
 */
class CombinStreams(val ins: InputStream *) extends InputStream{
    
    val streamIter = ins.iterator
    var in: InputStream = streamIter.next()

  override def read(): Int = {
      var cur = in.read()
      while(cur < 0 && streamIter.hasNext) {
          in = streamIter.next()
          cur = in.read()
      }
      cur
    }
  
}