package mega.privacy.android.core.sharedcomponents.extension

/**
 * Shortens a string that is longer than [maxLength] to its first and last [edgeLength]
 * characters joined by an ellipsis (e.g. `"FirstBlock...FinalBlock"`). Strings within the
 * limit are returned unchanged.
 *
 * @param maxLength the length above which the string is truncated.
 * @param edgeLength the number of leading and trailing characters to keep when truncating.
 */
fun String.truncateMiddle(maxLength: Int = 20, edgeLength: Int = 8): String =
    if (length > maxLength) {
        "${take(edgeLength)}...${takeLast(edgeLength)}"
    } else {
        this
    }
