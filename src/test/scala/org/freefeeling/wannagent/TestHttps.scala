package org.freefeeling.wannagent

import java.io.{InputStreamReader, BufferedReader, IOException}
import java.net.{InetSocketAddress, URI, URL}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, SocketChannel, Channel}
import javax.net.ssl.SSLSocketFactory

import com.sun.corba.se.spi.orb.OperationFactory

import scala.util.Try

/**
  * Created by zhenghu on 15-11-13.
  */
object TestHttps {

  def testUrl() {
    val githubURL = new URL("https://www.github.com")
    val httpsConn = githubURL.openConnection()
    println(httpsConn.getHeaderFields)
    val ins = httpsConn.getInputStream
    val cache = new Array[Byte](100)
    while ( {
      val size = ins.read(cache);
      if (size > 0) System.out.write(cache, 0, size);
      size > 0
    }) {}
  }

  val msg = "CONNECT github.com:443 HTTP/1.1\r\n" +
    "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0\r\n" +
    "Proxy-Connection: keep-alive\r\n" +
    "Connection: keep-alive\r\n" +
    "Host: github.com:443\r\n" +
    "\r\n"

  var msg2 = "GET https://github.com/ HTTP/1.1\r\n" +
    "Host: github.com\r\n" +
    "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0\r\n" +
    "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
    "Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3\nAccept-Encoding: gzip, deflate\r\n" +
    "Cookie: logged_in=no; _ga=GA1.2.1112653799.1425278990; _octo=GH1.1.135954816.1425278991\r\n" +
    "Connection: keep-alive\r\n" +
    "Cache-Control: max-age=0\r\n" +
    "\r\n"
  testSslSocket()

  def testSslSocket(): Unit = {
    val sslFactory = SSLSocketFactory.getDefault()
    val socket = sslFactory.createSocket("github.com", 443)
    val ous = socket.getOutputStream
    ous.write(msg2.getBytes("UTF-8"))
    ous.flush()

    val ins = socket.getInputStream

    val cache = new Array[Byte](100)
    while ( {
      val size = ins.read(cache);
      if (size > 0) System.out.write(cache, 0, size);
      size > 0
    }) {}
  }

  def testSslSocketChannel(): Unit = {
    val sslFactory = SSLSocketFactory.getDefault()
    val socket = sslFactory.createSocket("github.com", 443)
    val chnl = socket.getChannel()
    chnl.configureBlocking(false)

    val selector = Selector.open()
    chnl.register(selector, SelectionKey.OP_WRITE)
    chnl.register(selector, SelectionKey.OP_READ)
    import Main._

    select(selector) { key =>
      if (key.isWritable()) {
        println("writable")
        key.channel() match {
          case conn: SocketChannel =>
            val buffer = ByteBuffer.wrap(msg.getBytes())
            conn.write(buffer)
        }

      } else if (key.isReadable()) {
        println("readable")
        key.channel() match {
          case conn: SocketChannel =>
            val buffer = ByteBuffer.allocate(1024)
            while (conn.read(buffer) > -1) {
              buffer.flip()
              System.out.write(buffer)
              buffer.flip()
            }
        }
      }
    }
  }

  def testSocketChannel() {
    val address = new InetSocketAddress("github.com", 443)

    val chnl = SocketChannel.open()
    chnl.connect(address)
    chnl.configureBlocking(false)

    val selector = Selector.open()
    chnl.register(selector, SelectionKey.OP_WRITE)
    chnl.register(selector, SelectionKey.OP_READ)
    import Main._

    select(selector) { key =>
      if (key.isWritable()) {
        println("writable")
        key.channel() match {
          case conn: SocketChannel =>
            val buffer = ByteBuffer.wrap(msg.getBytes())

            conn.write(buffer)

        }

      } else if (key.isReadable()) {
        println("readable")
        key.channel() match {
          case conn: SocketChannel =>
            val buffer = ByteBuffer.allocate(1024)
            while (conn.read(buffer) > -1) {
              buffer.flip()
              System.out.write(buffer)
              buffer.flip()
            }
        }
      }
    }
  }
}
