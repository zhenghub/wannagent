package org.freefeeling.wannagent;

import java.io.*;
import java.net.Socket;
import java.security.*;
import javax.net.ssl.*;
public class HttpsHello {
    // 启动一个ssl server socket
// 配置了证书, 所以不会抛出异常
    public static void sslSocketServer() throws Exception {

        // key store相关信息
        String keyName = "wannagent.jks";
        char[] keyStorePwd = "111aaa".toCharArray();
        char[] keyPwd = "111aaa".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        // 装载当前目录下的key store. 可用jdk中的keytool工具生成keystore
        InputStream in = new FileInputStream(keyName);
        keyStore.load(in, keyPwd);
        in.close();

        // 初始化key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(keyStore, keyPwd);

        // 初始化ssl context
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(kmf.getKeyManagers(), null, null);

        // 监听和接收客户端连接
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket) factory
                .createServerSocket(10002);
        System.out.println("ok");
        Socket client = server.accept();
        System.out.println(client.getRemoteSocketAddress());

        // 向客户端发送接收到的字节序列
        OutputStream output = client.getOutputStream();

        // 当一个普通 socket 连接上来, 这里会抛出异常
        // Exception in thread "main" javax.net.ssl.SSLException: Unrecognized
        // SSL message, plaintext connection?
        InputStream input = client.getInputStream();
        byte[] buf = new byte[1024];
        int len = input.read(buf);
        System.out.println("received: " + new String(buf, 0, len));
        output.write(buf, 0, len);
        output.flush();
        output.close();
        input.close();

        // 关闭socket连接
        client.close();
        server.close();
    }

    public static void main(String[] args) throws Exception{
//        sslSocketServer();
        maina(null);
    }

    public static void maina(String[] args) {
        String ksName = "wannagent.jks";
        char ksPass[] = "111aaa".toCharArray();
        char ctPass[] = "111aaa".toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(ksName), ksPass);
            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, ctPass);
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory ssf = sc.getServerSocketFactory();
            SSLServerSocket s
                    = (SSLServerSocket) ssf.createServerSocket(10002);
            s.setEnabledCipherSuites(s.getSupportedCipherSuites());
            System.out.println("Server started:");
            printServerSocketInfo(s);
            // Listening to the port
            SSLSocket c = (SSLSocket) s.accept();
            printSocketInfo(c);
            BufferedWriter w = new BufferedWriter(
                    new OutputStreamWriter(c.getOutputStream()));
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream()));
            String m = r.readLine();
            w.write("HTTP/1.0 200 OK");
            w.newLine();
            w.write("Content-Type: text/html");
            w.newLine();
            w.newLine();
            w.write("<html><body>Hello world!</body></html>");
            w.newLine();
            w.flush();
            w.close();
            r.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void printSocketInfo(SSLSocket s) {
        System.out.println("Socket class: "+s.getClass());
        System.out.println("   Remote address = "
                +s.getInetAddress().toString());
        System.out.println("   Remote port = "+s.getPort());
        System.out.println("   Local socket address = "
                +s.getLocalSocketAddress().toString());
        System.out.println("   Local address = "
                +s.getLocalAddress().toString());
        System.out.println("   Local port = "+s.getLocalPort());
        System.out.println("   Need client authentication = "
                +s.getNeedClientAuth());
        SSLSession ss = s.getSession();
        System.out.println("   Cipher suite = "+ss.getCipherSuite());
        System.out.println("   Protocol = "+ss.getProtocol());
    }
    private static void printServerSocketInfo(SSLServerSocket s) {
        System.out.println("Server socket class: "+s.getClass());
        System.out.println("   Socket address = "
                +s.getInetAddress().toString());
        System.out.println("   Socket port = "
                +s.getLocalPort());
        System.out.println("   Need client authentication = "
                +s.getNeedClientAuth());
        System.out.println("   Want client authentication = "
                +s.getWantClientAuth());
        System.out.println("   Use client mode = "
                +s.getUseClientMode());
    }
}