package org.freefeeling.wannagent

import java.net.ServerSocket
import java.net.Socket
import org.freefeeling.wannagent.http.HttpConnection
import com.sun.net.httpserver.spi.HttpServerProvider
import java.net.InetAddress
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
import java.nio.channels.ServerSocketChannel
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import org.freefeeling.wannagent.http.HttpHeader
import scala.collection.convert.decorateAsScala._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import Main.{ select, Address }
import scala.actors.Actor

/**
  * @author zh
  */
object TestFrontRequest extends App {

    val port = 36446
    import sys.process._

    def run() {
        val serverChannel = ServerSocketChannel.open()
        serverChannel.configureBlocking(false)
        serverChannel.socket().bind(new InetSocketAddress(port))
        val frontSelector = Selector.open()
        serverChannel.register(frontSelector, SelectionKey.OP_ACCEPT)
        val channelMap: concurrent.Map[SocketChannel, Channel] = new ConcurrentHashMap().asScala

        case class Channel(val client: SocketChannel, val remoteHost: String, val remotePort: Int)
        import scala.collection.JavaConversions.asScalaSet
        case class ResultException(host: String, port: Int) extends RuntimeException

        var address: Address = null
        def listen4Request() {
            def frontSelect(key: SelectionKey) {
                if (key.isAcceptable()) {
                    key.channel() match {
                        case server: ServerSocketChannel =>
                            val conn = server.accept()
                            conn.configureBlocking(false)
                            conn.register(frontSelector, SelectionKey.OP_READ)
                    }
                } else if (key.isReadable()) {
                    key.channel() match {
                        case conn: SocketChannel =>
                            val connInput = new ChannelInputStream(conn)
                            val header = HttpHeader.readHeader(connInput)
                            throw ResultException(header.host, header.port)
                        case _ =>
                    }
                }
            }
            try {
                select(frontSelector, frontSelect _)
            } catch {
                case ResultException(host, port) =>
                    address = Address(host, port)
            }
        }


        import Debug._
        listen4Request _ start

        val client = new Thread {
            override def run() {
                val processBuilder: ProcessBuilder = s"""curl http://www.baidu.com -x localhost:${port}"""
                val process = processBuilder.run()
                Thread.sleep(1000)
                process.destroy()
                print(address)
                assert (address == Address("www.baidu.com", 80))
            }
        }
        client start
    }

    run()
}