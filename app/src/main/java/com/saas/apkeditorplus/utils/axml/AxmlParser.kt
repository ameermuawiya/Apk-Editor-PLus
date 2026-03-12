package com.saas.apkeditorplus.utils.axml

import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.io.Reader

class AxmlParser : XmlResourceParser {

    private var stream: AxmlStream? = null
    private var stringBlock: AxmlStringBlock? = null
    private var resourceIds: IntArray? = null
    private val namespaceStack = AxmlNamespaceStack()
    
    private var eventType: Int = XmlPullParser.START_DOCUMENT
    private var lineNumber: Int = -1
    private var name: Int = -1
    private var namespace: Int = -1
    private var attributes: IntArray? = null
    private var idAttributeIndex: Int = -1
    private var classAttributeIndex: Int = -1
    private var styleAttributeIndex: Int = -1
    private var isOpened: Boolean = false

    constructor()

    constructor(inputStream: InputStream) {
        open(inputStream)
    }

    fun open(inputStream: InputStream) {
        close()
        stream = AxmlStream(inputStream)
        isOpened = true
    }

    @Throws(IOException::class)
    private fun doNext() {
        if (stringBlock == null) {
            val header = stream?.readInt()
            if (header != 0x00080003) throw IOException("Invalid AXML header")
            stream?.readInt() // size
            stringBlock = AxmlStringBlock()
            stringBlock?.read(stream!!)
        }

        while (true) {
            val type = stream?.readInt() ?: -1
            if (type == -1) {
                eventType = XmlPullParser.END_DOCUMENT
                return
            }
            val size = stream?.readInt()
            lineNumber = stream?.readInt() ?: -1
            stream?.readInt() // 0xFFFFFFFF

            when (type) {
                0x00080180 -> { // Resource IDs
                    resourceIds = stream?.readIntArray((size!! - 8) / 4)
                    continue
                }
                0x00100100 -> { // Start Namespace
                    val prefix = stream?.readInt() ?: -1
                    val uri = stream?.readInt() ?: -1
                    namespaceStack.add(prefix, uri)
                    continue
                }
                0x00100101 -> { // End Namespace
                    stream?.readInt() // prefix
                    stream?.readInt() // uri
                    namespaceStack.pop()
                    continue
                }
                0x00100102 -> { // Start Tag
                    namespace = stream?.readInt() ?: -1
                    name = stream?.readInt() ?: -1
                    stream?.readInt() // 0x00140014
                    val attrCount = stream?.readInt() ?: 0
                    idAttributeIndex = (stream?.readInt() ?: 1) - 1
                    classAttributeIndex = (stream?.readInt() ?: 1) - 1
                    styleAttributeIndex = (stream?.readInt() ?: 1) - 1
                    attributes = stream?.readIntArray(attrCount * 5)
                    eventType = XmlPullParser.START_TAG
                    return
                }
                0x00100103 -> { // End Tag
                    namespace = stream?.readInt() ?: -1
                    name = stream?.readInt() ?: -1
                    eventType = XmlPullParser.END_TAG
                    return
                }
                0x00100104 -> { // Text
                    name = stream?.readInt() ?: -1
                    stream?.readInt() // 0x00080008
                    stream?.readInt() // 0x00080008
                    eventType = XmlPullParser.TEXT
                    return
                }
            }
        }
    }

    override fun next(): Int {
        try {
            doNext()
            return eventType
        } catch (e: IOException) {
            close()
            throw e
        }
    }

    override fun getEventType(): Int = eventType
    override fun getLineNumber(): Int = lineNumber
    override fun getName(): String? = stringBlock?.getString(name)
    override fun getNamespace(): String? = stringBlock?.getString(namespace)
    override fun getPrefix(): String? = stringBlock?.getString(namespaceStack.findPrefix(namespace))
    
    override fun getAttributeCount(): Int = if (eventType == XmlPullParser.START_TAG) attributes!!.size / 5 else -1

    override fun getAttributeName(index: Int): String? {
        val i = getAttributeOffset(index)
        return stringBlock?.getString(attributes!![i + 1])
    }

    override fun getAttributeValue(index: Int): String? {
        val i = getAttributeOffset(index)
        val type = attributes!![i + 3]
        val data = attributes!![i + 4]
        if (type == 3) {
            return stringBlock?.getString(data)
        }
        return data.toString()
    }

    private fun getAttributeOffset(index: Int): Int {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("Not a START_TAG")
        val offset = index * 5
        if (offset >= attributes!!.size) throw IndexOutOfBoundsException("Invalid attribute index")
        return offset
    }

    // Standard XmlResourceParser methods (mostly empty or delegate)
    override fun close() {
        if (isOpened) {
            isOpened = false
            stream?.close()
            stream = null
            stringBlock = null
            resourceIds = null
            namespaceStack.reset()
        }
    }

    override fun getAttributeNamespace(index: Int): String? = stringBlock?.getString(attributes!![getAttributeOffset(index)])
    override fun getAttributePrefix(index: Int): String? = stringBlock?.getString(namespaceStack.findPrefix(attributes!![getAttributeOffset(index)]))
    override fun getAttributeType(index: Int): String = "CDATA"
    override fun getAttributeValue(namespace: String?, name: String?): String? = null
    override fun getAttributeNameResource(index: Int): Int {
        val i = attributes!![getAttributeOffset(index) + 1]
        if (resourceIds == null || i < 0 || i >= resourceIds!!.size) return 0
        return resourceIds!![i]
    }

    override fun getAttributeListValue(index: Int, options: Array<out String>?, defaultValue: Int): Int = 0
    override fun getAttributeBooleanValue(index: Int, defaultValue: Boolean): Boolean = getAttributeIntValue(index, if (defaultValue) 1 else 0) != 0
    override fun getAttributeResourceValue(index: Int, defaultValue: Int): Int {
        val i = getAttributeOffset(index)
        return if (attributes!![i + 3] == 1) attributes!![i + 4] else defaultValue
    }
    override fun getAttributeIntValue(index: Int, defaultValue: Int): Int {
        val i = getAttributeOffset(index)
        val type = attributes!![i + 3]
        return if (type in 16..31) attributes!![i + 4] else defaultValue
    }
    override fun getAttributeUnsignedIntValue(index: Int, defaultValue: Int): Int = getAttributeIntValue(index, defaultValue)
    override fun getAttributeFloatValue(index: Int, defaultValue: Float): Float {
        val i = getAttributeOffset(index)
        return if (attributes!![i + 3] == 4) java.lang.Float.intBitsToFloat(attributes!![i + 4]) else defaultValue
    }
    override fun getAttributeListValue(namespace: String?, attribute: String?, options: Array<out String>?, defaultValue: Int): Int = 0
    override fun getAttributeBooleanValue(namespace: String?, attribute: String?, defaultValue: Boolean): Boolean = false
    override fun getAttributeResourceValue(namespace: String?, attribute: String?, defaultValue: Int): Int = 0
    override fun getAttributeIntValue(namespace: String?, attribute: String?, defaultValue: Int): Int = 0
    override fun getAttributeUnsignedIntValue(namespace: String?, attribute: String?, defaultValue: Int): Int = 0
    override fun getAttributeFloatValue(namespace: String?, attribute: String?, defaultValue: Float): Float = 0f
    override fun getIdAttribute(): String? = if (idAttributeIndex == -1) null else getAttributeValue(idAttributeIndex)
    override fun getClassAttribute(): String? = if (classAttributeIndex == -1) null else getAttributeValue(classAttributeIndex)
    override fun getIdAttributeResourceValue(defaultValue: Int): Int = if (idAttributeIndex == -1) defaultValue else getAttributeResourceValue(idAttributeIndex, defaultValue)
    override fun getStyleAttribute(): Int = if (styleAttributeIndex == -1) 0 else attributes!![getAttributeOffset(styleAttributeIndex) + 4]

    // XmlPullParser methods
    override fun setFeature(name: String?, state: Boolean) {}
    override fun getFeature(name: String?): Boolean = false
    override fun setProperty(name: String?, value: Any?) {}
    override fun getProperty(name: String?): Any? = null
    override fun setInput(reader: Reader?) {}
    override fun setInput(inputStream: InputStream?, inputEncoding: String?) {}
    override fun getInputEncoding(): String? = null
    override fun defineEntityReplacementText(entityName: String?, replacementText: String?) {}
    override fun getNamespaceCount(depth: Int): Int = namespaceStack.getCount(depth)
    override fun getNamespacePrefix(pos: Int): String? = stringBlock?.getString(namespaceStack.getPrefix(pos))
    override fun getNamespaceUri(pos: Int): String? = stringBlock?.getString(namespaceStack.getUri(pos))
    override fun getNamespace(prefix: String?): String? = null
    override fun getDepth(): Int = namespaceStack.getCurrentDepth()
    override fun getPositionDescription(): String = "XML line #$lineNumber"
    override fun getColumnNumber(): Int = -1
    override fun isWhitespace(): Boolean = false
    override fun getText(): String? = if (eventType == XmlPullParser.TEXT) stringBlock?.getString(name) else null
    override fun getTextCharacters(holderForStartAndLength: IntArray?): CharArray? {
        val t = text ?: return null
        holderForStartAndLength?.set(0, 0)
        holderForStartAndLength?.set(1, t.length)
        return t.toCharArray()
    }
    override fun isAttributeDefault(index: Int): Boolean = false
    override fun nextToken(): Int = next()
    override fun require(type: Int, namespace: String?, name: String?) {
        if (type != eventType || (namespace != null && namespace != getNamespace()) || (name != null && name != getName())) {
            throw XmlPullParserException("Expected $type, $namespace, $name")
        }
    }
    override fun nextText(): String? {
        if (eventType != XmlPullParser.START_TAG) throw XmlPullParserException("Parser must be on START_TAG")
        var type = next()
        if (type == XmlPullParser.TEXT) {
            val result = text
            type = next()
            if (type != XmlPullParser.END_TAG) throw XmlPullParserException("TEXT must be followed by END_TAG")
            return result
        }
        if (type == XmlPullParser.END_TAG) return ""
        throw XmlPullParserException("Parser must be on START_TAG or TEXT")
    }
    override fun nextTag(): Int {
        var type = next()
        if (type == XmlPullParser.TEXT && isWhitespace) type = next()
        if (type != XmlPullParser.START_TAG && type != XmlPullParser.END_TAG) throw XmlPullParserException("Expected START_TAG or END_TAG")
        return type
    }
    override fun isEmptyElementTag(): Boolean = false
}
