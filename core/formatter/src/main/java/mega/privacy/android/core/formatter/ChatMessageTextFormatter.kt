package mega.privacy.android.core.formatter

/**
 * Removes the MEGA chat formatting tags from a message, returning the plain text to
 * display where styled spans are not supported, such as a system notification.
 *
 * Supported tags are `*bold*`, `_italic_`, `~strikethrough~`, `` `quote` `` and the
 * multi-line ` ```quote``` `. A tag is only removed when it has a closing counterpart
 * with at least one character in between and, except for the multi-line quote, no line
 * break between them. Tags that do not form such a pair are kept as they were typed.
 */
fun String.stripChatFormatTags(): String {
    val plainText = StringBuilder(length)
    var index = 0

    while (index < length) {
        val char = this[index]

        if (char !in FORMAT_TAGS) {
            plainText.append(char)
            index++
            continue
        }

        if (startsWith(MULTI_QUOTE_TAG, index)) {
            val contentStart = index + MULTI_QUOTE_TAG.length
            val closingIndex = indexOf(MULTI_QUOTE_TAG, contentStart)

            if (closingIndex > contentStart) {
                plainText.append(substring(contentStart, closingIndex))
                index = closingIndex + MULTI_QUOTE_TAG.length
                continue
            }
        } else {
            val closingIndex = indexOf(char, index + 1)

            if (closingIndex > index + 1 && !substring(index + 1, closingIndex).contains('\n')) {
                plainText.append(substring(index + 1, closingIndex).stripChatFormatTags())
                index = closingIndex + 1
                continue
            }
        }

        plainText.append(char)
        index++
    }

    return plainText.toString()
}

private const val FORMAT_TAGS = "*_~`"
private const val MULTI_QUOTE_TAG = "```"
