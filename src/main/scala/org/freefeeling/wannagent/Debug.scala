package org.freefeeling.wannagent

/**
 * @author zh
 */
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream
object Debug {

    def printInputStream(in: InputStream) {
        val reader = new BufferedReader(new InputStreamReader(in))
        while ({ val line = reader.readLine(); println(line); line != null }) {}
    }

    implicit def func2Thread(func: () => Unit) = new Thread {
        override def run =
            func()
    }
}