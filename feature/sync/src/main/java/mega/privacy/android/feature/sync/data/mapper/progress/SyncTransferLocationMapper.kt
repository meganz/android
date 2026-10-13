package mega.privacy.android.feature.sync.data.mapper.progress

import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Returns the name of the folder that holds a sync transfer path, or "" when it has none.
 * String-only: SAF sync paths append unencoded names to a content URI, so only the root's
 * encoded id is decoded.
 */
internal class SyncTransferLocationMapper @Inject constructor() {

    operator fun invoke(path: String?): String {
        val parent = path?.substringBeforeLast('/', missingDelimiterValue = "").orEmpty()
        if (parent.isEmpty()) return ""
        val rootId = contentRootId(parent)
        return if (rootId != null) folderNameOf(percentDecode(rootId)) else parent.substringAfterLast('/')
    }

    /** Returns the encoded id of a content-URI root when [parent] is exactly that root, else null. */
    private fun contentRootId(parent: String): String? {
        if (!parent.startsWith(CONTENT_SCHEME)) return null
        val treeStart = parent.indexOf(TREE_SEGMENT).takeIf { it >= 0 } ?: return null
        val treeIdStart = treeStart + TREE_SEGMENT.length
        val treeIdEnd = parent.segmentEnd(treeIdStart)
        val (idStart, idEnd) = if (parent.startsWith(DOCUMENT_SEGMENT, treeIdEnd)) {
            val documentIdStart = treeIdEnd + DOCUMENT_SEGMENT.length
            documentIdStart to parent.segmentEnd(documentIdStart)
        } else {
            treeIdStart to treeIdEnd
        }
        return if (idEnd == parent.length) parent.substring(idStart, idEnd) else null
    }

    private fun String.segmentEnd(start: Int): Int = indexOf('/', start).let { if (it < 0) length else it }

    private fun folderNameOf(documentId: String): String =
        documentId.substringAfter(':').trimEnd('/').substringAfterLast('/')
            .ifEmpty { documentId.substringBefore(':') }

    private companion object {
        const val CONTENT_SCHEME = "content://"
        const val TREE_SEGMENT = "/tree/"
        const val DOCUMENT_SEGMENT = "/document/"
    }
}

/**
 * Decodes each run of `%XX` escapes as UTF-8 and keeps an invalid escape and `+` as written.
 * Only the root id is decoded because the SDK appends the names below the root without encoding
 * them, so a URI parser or a form decoder would corrupt a name that holds `%`, `#` or `+`.
 */
private fun percentDecode(text: String): String {
    if ('%' !in text) return text
    val result = StringBuilder(text.length)
    val bytes = ByteArrayOutputStream()
    var index = 0
    while (index < text.length) {
        val byte = text.escapedByteAt(index)
        if (byte != null) {
            bytes.write(byte)
            index += ESCAPE_LENGTH
        } else {
            result.flushBytes(bytes)
            result.append(text[index])
            index++
        }
    }
    result.flushBytes(bytes)
    return result.toString()
}

private fun String.escapedByteAt(index: Int): Int? {
    if (this[index] != '%' || index + ESCAPE_LENGTH > length) return null
    val high = hexValue(this[index + 1]) ?: return null
    val low = hexValue(this[index + 2]) ?: return null
    return high * HEX_RADIX + low
}

private fun hexValue(char: Char): Int? = when (char) {
    in '0'..'9' -> char - '0'
    in 'a'..'f' -> char - 'a' + DECIMAL_RADIX
    in 'A'..'F' -> char - 'A' + DECIMAL_RADIX
    else -> null
}

private fun StringBuilder.flushBytes(bytes: ByteArrayOutputStream) {
    if (bytes.size() == 0) return
    append(bytes.toByteArray().toString(Charsets.UTF_8))
    bytes.reset()
}

private const val ESCAPE_LENGTH = 3
private const val HEX_RADIX = 16
private const val DECIMAL_RADIX = 10
