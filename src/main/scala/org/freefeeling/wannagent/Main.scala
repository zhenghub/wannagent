package org.freefeeling.wannagent

import java.net.ServerSocket
import java.net.Socket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import javax.net.ssl.SSLSocketFactory
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.scalalogging.{Logger, Logging}
import http.{HttpHeader, HttpConnection}
import org.slf4j.LoggerFactory
import scala.collection.convert.decorateAsScala.mapAsScalaConcurrentMapConverter
import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import java.nio.channels.WritableByteChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.Channels
import java.io.{IOException, OutputStream, InputStream, ByteArrayInputStream}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author zhenghu
 */
object Main extends App with Logging {
    val logger = Logger(LoggerFactory getLogger "stdout")

    logger.info("hello world");
    val port = 9999

    def select(selector: Selector)(func: (SelectionKey) => Unit) {
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

    var msg2 = "GET https://github.com/ HTTP/1.1\r\n" +
      "Host: github.com\r\n" +
      "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0\r\n" +
      "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
      "Accept-Language: zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3\nAccept-Encoding: gzip, deflate\r\n" +
      "Cookie: logged_in=no; _ga=GA1.2.1112653799.1425278990; _octo=GH1.1.135954816.1425278991\r\n" +
      "Connection: keep-alive\r\n" +
      "Cache-Control: max-age=0\r\n" +
      "\r\n"

    def request(address: InetSocketAddress, inc: ReadableByteChannel, out: WritableByteChannel) {
        val socket = address.getPort match {
            case 443 =>
                val sslFactory = SSLSocketFactory.getDefault()
                sslFactory.createSocket(address.getHostName, 443)
            case _ =>
                val s = new Socket()
                s.connect(address)
                s
        }
        val ous = Channels.newChannel(socket.getOutputStream)
        val size = 1024
        val buffer = ByteBuffer.allocate(size)

        val sous = Channels.newChannel(System.out)
        var reads = 0
        var in = Channels.newChannel(new ByteArrayInputStream(msg2.getBytes))
        while ({ reads = in.read(buffer); reads > 0 }) {
            buffer.flip()
            val buf = new Array[Byte](buffer.remaining())
            buffer.get(buf, 0, buf.length);
            sous.write(ByteBuffer.wrap(buf))

            Try(ous.write(ByteBuffer.wrap(buf))) recover {
                case e: IOException =>
                    logger.error("back request can't write", e)
                    in.close()
                    out.close()
                    return
            }
            buffer.flip()
        }

        val ins = Channels.newChannel(socket.getInputStream)

        while (
            Try(ins.read(buffer) > -1) recover {
                case e: IOException => logger.error("back request can't read", e); false
            } get
        ) {
            buffer.flip()
            val buf = new Array[Byte](buffer.remaining())
            buffer.get(buf, 0, buf.length);

            out.write(ByteBuffer.wrap(buf))
            sous.write(ByteBuffer.wrap(buf))
            //            out.write(buffer)
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
        val channelMap: concurrent.Map[SocketChannel, Channel] = new ConcurrentHashMap[SocketChannel, Channel]().asScala

        case class Channel(val client: SocketChannel, val remoteHost: String, val remotePort: Int)

        def listen4Request() {
            def frontSelect(key: SelectionKey) {
                if (key.isAcceptable()) {
                    key.channel() match {
                        case server: ServerSocketChannel =>
                            val conn = server.accept()
                            conn.configureBlocking(false)
                            conn.register(frontSelector, SelectionKey.OP_READ)
                            logger.debug("new socket connected: " + conn.getRemoteAddress);
                        case other =>
                            logger.debug("unkown channel connected: " + other)
                    }
                } else if (key.isReadable()) {
                    key.channel() match {
                        case conn: SocketChannel =>
                            val connInput = new ChannelInputStream(conn)
                            logger.debug(s"socket ${conn.getRemoteAddress} is readable")
                            Try(HttpHeader.readHeader(connInput)) match {
                                case Success(header) =>
                                    val in = new CombinedStreams(new ByteArrayInputStream(header.toBytes()), connInput)
                                    Future {
                                        request(new InetSocketAddress(header.host, header.port), in, conn)
                                    } onFailure {
                                        case e => logger.error(s"process request to (${header.host}, ${header.port})error", e)
                                    }
                                case Failure(e) =>
                                    logger.debug("a front request error", e)
                                    key.cancel()
                                    conn.close()
                            }
                        case ch =>
                            logger.debug("unkown readable channel" + ch)
                    }
                }
            }
            select(frontSelector)(frontSelect _)
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
            select(backSelector)(backSelect _)
        }
        implicit def func2Thread(func: => Unit) =
            new Thread {
                override def run = func
            }
        listen4Request start;
    }

    run()

}