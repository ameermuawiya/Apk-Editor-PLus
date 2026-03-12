package com.saas.apkeditorplus.utils.axml

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

class AxmlStream(private var inputStream: InputStream?, private val bigEndian: Boolean = false) {

    private var position: Int = 0

    @Throws(IOException::class)
    fun close() {
        inputStream?.close()
        inputStream = null
        position = 0
    }

    @Throws(IOException::class)
    fun readBytes(buffer: ByteArray) {
        var readCount = 0
        val length = buffer.size
        while (readCount < length) {
            val count = inputStream?.read(buffer, readCount, length - readCount) ?: -1
            if (count == -1) throw EOFException()
            readCount += count
            position += count
        }
    }

    @Throws(IOException::class)
    fun readIntArray(count: Int): IntArray {
        val array = IntArray(count)
        for (i in 0 until count) {
            array[i] = readInt()
        }
        return array
    }

    @Throws(IOException::class)
    fun readByte(): Int {
        val b = inputStream?.read() ?: -1
        if (b == -1) throw EOFException()
        position++
        return b
    }

    @Throws(IOException::class)
    fun readInt(): Int {
        return readInt(4)
    }

    @Throws(IOException::class)
    private fun readInt(size: Int): Int {
        var result = 0
        if (bigEndian) {
            for (i in (size - 1) downTo 0) {
                val b = inputStream?.read() ?: -1
                if (b == -1) throw EOFException()
                position++
                result = result or (b shl (i * 8))
            }
        } else {
            for (i in 0 until size) {
                val b = inputStream?.read() ?: -1
                if (b == -1) throw EOFException()
                position++
                result = result or (b shl (i * 8))
            }
        }
        return result
    }

    @Throws(IOException::class)
    fun skipInt() {
        val skipped = inputStream?.skip(4L) ?: 0L
        position += skipped.toInt()
        if (skipped != 4L) throw EOFException()
    }
}
