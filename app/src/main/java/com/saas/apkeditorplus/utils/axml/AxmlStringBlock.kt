package com.saas.apkeditorplus.utils.axml

import android.util.SparseArray
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset

class AxmlStringBlock {

    private var stringOffsets: IntArray? = null
    private var stringsData: ByteArray? = null
    private var isUtf8: Boolean = false
    private var styleOffsets: IntArray? = null

    private val stringCache = SparseArray<String>()
    private val utf16Decoder = Charset.forName("UTF-16LE").newDecoder()
    private val utf8Decoder = Charset.forName("UTF-8").newDecoder()

    @Throws(IOException::class)
    fun read(stream: AxmlStream) {
        // Chunk type and size already read or handled by caller if needed
        // But the original code expects them to be checked
        val chunkType = stream.readInt()
        if (chunkType != 0x001C0001) {
            throw IOException(String.format("Expected: 0x%08x, actual: 0x%08x", 0x001C0001, chunkType))
        }

        val chunkSize = stream.readInt()
        val stringCount = stream.readInt()
        val styleCount = stream.readInt()
        val flags = stream.readInt()
        val stringsOffset = stream.readInt()
        val stylesOffset = stream.readInt()

        isUtf8 = (flags and 256) != 0
        stringOffsets = stream.readIntArray(stringCount)
        
        if (styleCount != 0) {
            styleOffsets = stream.readIntArray(styleCount)
        }

        val size = (if (stylesOffset == 0) chunkSize else stylesOffset) - stringsOffset
        stringsData = ByteArray(size)
        stream.readBytes(stringsData!!)

        if (stylesOffset != 0) {
            val size2 = chunkSize - stylesOffset
            val stylesData = IntArray(size2 / 4)
            // Just skip or read if needed, the original code mostly skips
            for (i in 0 until (size2 / 4)) {
                stream.readInt()
            }
        }
    }

    fun getString(index: Int): String? {
        if (index < 0 || stringOffsets == null || index >= stringOffsets!!.size) {
            return null
        }
        val cached = stringCache.get(index)
        if (cached != null) return cached

        val offset = stringOffsets!![index]
        val string: String?
        if (isUtf8) {
            val pos = getUtf8Pos(offset)
            string = decodeString(pos[0], pos[1])
        } else {
            val pos = getUtf16Pos(offset)
            string = decodeString(pos[0], pos[1])
        }
        
        if (string != null) {
            stringCache.put(index, string)
        }
        return string
    }

    private fun getUtf8Pos(offset: Int): IntArray {
        val data = stringsData!!
        var i = offset
        var length = data[i].toInt() and 0xFF
        if (length and 0x80 != 0) {
            i++
            length = (length and 0x7F shl 8) or (data[i].toInt() and 0xFF)
        }
        i++
        
        var bytes = data[i].toInt() and 0xFF
        if (bytes and 0x80 != 0) {
            i++
            bytes = (bytes and 0x7F shl 8) or (data[i].toInt() and 0xFF)
        }
        i++
        return intArrayOf(i, bytes)
    }

    private fun getUtf16Pos(offset: Int): IntArray {
        val data = stringsData!!
        var i = offset
        var length = (data[i + 1].toInt() and 0xFF shl 8) or (data[i].toInt() and 0xFF)
        if (length and 0x8000 != 0) {
            i += 2
            length = (length and 0x7FFF shl 16) or ((data[i + 1].toInt() and 0xFF shl 8) or (data[i].toInt() and 0xFF))
        }
        i += 2
        return intArrayOf(i, length * 2)
    }

    private fun decodeString(offset: Int, length: Int): String? {
        return try {
            (if (isUtf8) utf8Decoder else utf16Decoder)
                .decode(ByteBuffer.wrap(stringsData!!, offset, length)).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun findString(str: String?): Int {
        if (str == null || stringOffsets == null) return -1
        for (i in stringOffsets!!.indices) {
            if (getString(i) == str) return i
        }
        return -1
    }
}
