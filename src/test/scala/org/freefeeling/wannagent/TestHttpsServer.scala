package org.freefeeling.wannagent

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL
import javax.net.ssl.{SSLContext, SSLSocket, SSLSocketFactory}

import scala.io.Source

object TestHttpsServer {
  def sslSocket() {
    // set certificate keystore
    System.setProperty("javax.net.ssl.trustStore", "cmkey")
    val context = SSLContext.getInstance("SSL");
    context.init(null,   null,
    null)
    val factory = context.getSocketFactory();

    val s = factory.createSocket("localhost", 10002);
    System.out.println("ok");

    val output = s.getOutputStream();
    val input = s.getInputStream();

    output.write("alert".getBytes());
    System.out.println("sent: alert");
    output.flush();

    val buf = new Array[Byte](1024)
    val len = input.read(buf);
    System.out.println("received:" + new String(buf, 0, len));
  }

  def main(args: Array[String]) {
//    sslSocket()
    main2(null)
  }
  def main2(args: Array[String]) {
    System.setProperty("javax.net.ssl.trustStore", "wannagent.jks")
    val res = Source.fromInputStream(new URL("https://zh:10002").openStream()).getLines();
    res.foreach(println(_))
  }
}