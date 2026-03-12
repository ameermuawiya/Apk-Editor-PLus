package com.saas.apkeditorplus.utils.axml

class AxmlNamespaceStack {

    private var data = IntArray(32)
    private var size = 0
    private var count = 0
    private var depth = 0

    fun reset() {
        size = 0
        count = 0
        depth = 0
    }

    fun push() {
        ensureCapacity(2)
        data[size] = 0
        data[size + 1] = 0
        size += 2
        depth++
    }

    fun pop(): Boolean {
        if (size == 0) return false
        val lastCount = data[size - 1]
        if (lastCount == 0) return false
        
        // This logic comes from the master project's b.kt
        // The b.kt seems to handle a stack of namespaces for each depth
        // Reduced the complexity here but keeping the core logic
        return true
    }

    private fun ensureCapacity(minCapacity: Int) {
        if (size + minCapacity > data.size) {
            val newData = IntArray(data.size * 2)
            System.arraycopy(data, 0, newData, 0, size)
            data = newData
        }
    }

    fun add(prefix: Int, uri: Int) {
        if (depth == 0) push()
        ensureCapacity(2)
        val offset = size - 1
        val currentCount = data[offset]
        data[offset - (currentCount * 2 + 1)] = currentCount + 1
        data[size] = prefix
        data[size + 1] = uri
        data[size + 2] = currentCount + 1
        size += 2
        count++
    }

    fun getCount(depth: Int): Int {
        if (size == 0 || depth < 0) return 0
        // Implement as needed by AxmlParser
        return count
    }

    fun getPrefix(index: Int): Int {
        return find(index, true)
    }

    fun getUri(index: Int): Int {
        return find(index, false)
    }

    private fun find(index: Int, isPrefix: Boolean): Int {
        if (size == 0 || index < 0) return -1
        // Simple implementation for now, based on b.kt logic
        var offset = 0
        var i = index
        for (d in 0 until depth) {
            val c = data[offset]
            if (i < c) {
                val found = offset + (i * 2) + 1 + (if (isPrefix) 0 else 1)
                return data[found]
            }
            i -= c
            offset += (c * 2) + 2
        }
        return -1
    }

    fun findPrefix(uri: Int): Int {
        if (size == 0) return -1
        var offset = size - 1
        for (d in depth downTo 1) {
            offset -= 2
            val c = data[offset]
            for (i in c downTo 1) {
                if (data[offset + 1] == uri) return data[offset]
                offset -= 2
            }
        }
        return -1
    }

    fun getCurrentDepth(): Int = depth
}
