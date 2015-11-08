package org.freefeeling.wannagent

import java.nio.channels.SocketChannel
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.nio.channels.Channel
import java.nio.channels.WritableByteChannel
import java.nio.channels.ReadableByteChannel
import java.io.OutputStream

/*
 * @author zh
 */
object TestBackRequest extends App {
    val test = "GET HTTP://www.baidu.com/ HTTP/1.1\r\n" +
      "User-Agent: curl/7.37.1\r\n" +
      "Host: www.baidu.com\r\n" +
      "Accept: */*\r\n" +
      "Proxy-Connection: Keep-Alive\r\n\r\n"

    import Main._
    {
        println("isWritable")
        val bytes = test.getBytes
        request(new InetSocketAddress("www.baidu.com", 80), new ByteArrayInputStream(bytes), System.out)
    }
}