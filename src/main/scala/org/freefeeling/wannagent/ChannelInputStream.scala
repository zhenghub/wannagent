package org.freefeeling.wannagent

import java.io.InputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * @author zh
 */
class ChannelInputStream(val channel: SocketChannel, val cacheSize: Int = 4096) extends InputStream {

    val buffer = ByteBuffer.allocate(cacheSize)
    var array: Array[Byte] = _
    var pos = 0
    var finished = -1

    def read: Int = {
        if (empty) {
            fill()
            if (finished <= 0)
                return -1
            pos = 0
        }
        val res = array(pos)
        pos += 1
        res
    }

    def empty = array == null || pos == array.length - 1

    def fill() {
        finished = channel.read(buffer)
        if (finished > 0)
            array = buffer.array().clone()
        buffer.clear()
    }

    override def close() {
    }
}