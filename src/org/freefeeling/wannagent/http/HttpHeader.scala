package org.freefeeling.wannagent.http

import scala.collection.mutable.ArrayBuffer
import collection.mutable.Map
import com.sun.org.apache.xalan.internal.xsltc.trax.OutputSettings
import java.io.OutputStream
import java.io.OutputStreamWriter
import scala.collection.mutable.Map
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream

/**
 * @author zh
 */

object HttpHeader {
    def printChar(char: Char) {
        char match {
            case '\r' => print("""\r""")
            case '\n' => println("""\n""")
            case _    => print(char.toChar)
        }
    }
    
    def readHeader(in: InputStream) = {
        val connReader = new InputStreamReader(in)
        val header = ArrayBuffer[String]()
        var sb = new StringBuilder()
        var duringHeader = true
        val properties = Map[String, String]()
        var keySepPos: Int = -1
        def read = { val ch = connReader.read(); if (ch > -1) printChar(ch.toChar); ch }
        var char = read
        while (duringHeader && char > -1) {
            char match {
                case '\r' =>
                    char = read
                    if (char == '\n') {
                        header += sb.toString()
                        if (keySepPos > -1) {
                            properties(sb.substring(0, keySepPos).toLowerCase()) = sb.substring(keySepPos + 1).trim()
                            keySepPos = -1
                        }
                        if (sb.length == 0) {
                            duringHeader = false
                        } else {
                            sb = new StringBuilder
                        }
                    } else {
                        sb += '\r'
                        sb += char.toChar
                    }
                case _ =>
                    if (char == ':' && keySepPos == -1)
                        keySepPos = sb.length
                    sb += char.toChar
            }
            if (duringHeader)
                char = read
        }
        println(properties)
        new HttpHeader(header, properties)
    }
}

class HttpHeader(val content: ArrayBuffer[String], val params: Map[String, String]) {

    val (host, port) = {
        val hostAPort = params("host").split(":")
        if (hostAPort.length == 1) {
            (hostAPort(0), 80)
        } else
            (hostAPort(0), hostAPort(1).toInt)
    }

    def copyTo(out: OutputStream) {
        val writer = new OutputStreamWriter(out)
        for (line <- content) {
            writer.write(line)
            writer.write("\r\n")
        }
        writer.write("\r\n")
        writer.flush()
    }
    
    def toBytes() = {
        val content = new ByteArrayOutputStream()
        copyTo(content)
        content.toByteArray()
    }
}