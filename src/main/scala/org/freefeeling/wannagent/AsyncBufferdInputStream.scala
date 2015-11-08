package org.freefeeling.wannagent

import java.io.InputStream
import java.io.IOException

/**
 * @author zh
 */

object AsyncBufferdInputStream {
    private val cacheSize = 4096
    class Cache(val size: Int) {
        var cache = new Array[Byte](size)
        var filled: Int = 0
        var used: Int = -1

        def +=(ele: Int) {
            cache(filled) = ele.toByte
            filled += 1
        }

        def -- = {
            used += 1
            if (used <= filled)
                cache(used)
            else -1
        }

        def full = filled + 1 == size
        def empty = used == filled
    }
}

/**
 * 阻塞确实很难
 */
class AsyncBufferdInputStream(in: InputStream) extends InputStream with Runnable {
    import AsyncBufferdInputStream._

    @volatile private var caching = newCache
    @volatile var running = true
    var cached: Cache = _
    var notEmpty = false
    var lock = new Object()

    val thread = new Thread(this)
    thread.start()

    def newCache = new Cache(cacheSize)

    def read(): Int = {
        if (cached == null || cached.empty) {
            lock.synchronized {
                if (notEmpty) {
                    cached = caching
                    caching = newCache
                    cached.notify()
                }
            }
        }
        if (cached == null || cached.empty) {
            -1
        } else
            cached --
    }

    override def close() {
        super.close()
        this.running = false
        this.thread.interrupt()
    }

    def run() {
        try {
            var r = in.read()
            while (running && r > -1) {
                lock.synchronized {
                    if (!this.caching.full)
                        this.caching += r
                }
                if (this.caching.full)
                    this.caching.wait()
                r = in.read()
            }
        } catch {
            case intrpt: InterruptedException =>
                println("interrupted")
                intrpt.printStackTrace()
            case io: IOException =>
                println("because of io")
                io.printStackTrace()
        }
    }

}