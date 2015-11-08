package org.freefeeling.wannagent.http

import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.mutable.ArrayBuffer
import collection.mutable.{ Map => MapBuffer }
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * @author zh
 */

object HttpConnection {
    def apply(socket: Socket) = new HttpConnection(socket)
}

class HttpConnection(socket: Socket) {
    val connReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    private val output = socket.getOutputStream

    var header: HttpHeader = _
    
    def closed = false
    
    
    def receive() {
    }

    def copyTo(another: HttpConnection) {
        copyTo(another.output)
    }
    
    def copyTo(out: OutputStream) {
        header.copyTo(out)
        val writer = new OutputStreamWriter(out)
        var char = connReader.read()
        while (char > -1) {
            writer.write(char)
        }
        writer.flush()
    }
}