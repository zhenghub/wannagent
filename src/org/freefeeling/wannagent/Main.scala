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
import java.nio.channels.WritableByteChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.Channels
import java.io.OutputStream
import java.io.InputStream
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import java.io.ByteArrayInputStream

/**
 * @author zhenghu
 */
object Main extends App {

    println("hello world");
    val port = 9999

    def select(selector: Selector, func: (SelectionKey) => Unit) {
        while (true) {
            selector.select()
            val itr = selector.selectedKeys().iterator()
            while (itr.hasNext()) {
                val key = itr.next()
                itr.remove()
                func(key)
            }
        }
    }

    def request(address: InetSocketAddress, in: ReadableByteChannel, out: WritableByteChannel) {
        val chnl = SocketChannel.open()
        chnl.connect(address)
        val size = 1024
        val buffer = ByteBuffer.allocate(size)

        var reads = 0
        while ({ reads = in.read(buffer); reads > 0 }) {
            buffer.flip()
            chnl.write(buffer)
            buffer.flip()
        }
        while ({ val size = chnl.read(buffer); size > -1 }) {
            buffer.flip()
            out.write(buffer)
            buffer.flip()
        }
    }
    implicit val instream2readChannel: (InputStream) => ReadableByteChannel = Channels.newChannel _
    implicit val outStream2writableChannel: (OutputStream) => WritableByteChannel = Channels.newChannel _

    case class Address(host: String, port: Int)

    def run() {
        val serverChannel = ServerSocketChannel.open()
        serverChannel.configureBlocking(false)
        serverChannel.socket().bind(new InetSocketAddress(port))
        val frontSelector = Selector.open()
        val backSelector = Selector.open()
        serverChannel.register(frontSelector, SelectionKey.OP_ACCEPT)
        val channelMap: concurrent.Map[SocketChannel, Channel] = new ConcurrentHashMap().asScala

        case class Channel(val client: SocketChannel, val remoteHost: String, val remotePort: Int)
        import scala.collection.JavaConversions.asScalaSet

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
                            val in  = new CombinStreams(new ByteArrayInputStream(header.toBytes()), connInput)
                            request(new InetSocketAddress(header.host, header.port), in, conn)
//                            val target = SocketChannel.open()
//                            target.configureBlocking(false)
//                            target.register(backSelector, SelectionKey.OP_CONNECT)
//                            target.connect(new InetSocketAddress(header.host, header.port))
//                            val channel = Channel(conn, header.host, header.port)
//                            channelMap(target) = channel
//                            Debug.printInputStream(connInput)
                        case _ =>

                    }
                }
            }
            select(frontSelector, frontSelect _)
        }

        def ask4Data() {
            def backSelect(key: SelectionKey) {
                if (key.isConnectable()) {
                    key.channel() match {
                        case conn: SocketChannel =>
                            if (conn.isConnectionPending()) {
                                conn.finishConnect()
                            }
                            conn.configureBlocking(false);
                            conn.register(backSelector, SelectionKey.OP_READ)
                            conn.register(backSelector, SelectionKey.OP_WRITE)
                    }
                } else if (key.isWritable()) {
                    val conn = key.channel().asInstanceOf[SocketChannel]
                    val chn = channelMap(conn)

                } else if (key.isReadable()) {

                }
            }
            select(backSelector, backSelect _)
        }
        implicit def func2Thread(func: => Unit) =
            new Thread {
                override def run = func
            }
        listen4Request start;
    }

    run()

}