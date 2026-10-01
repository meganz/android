package mega.privacy.android.shared.nodes.namecollision.view

private const val ELLIPSIS = "…"
private const val MAX_EXTENSION_LENGTH = 8

/**
 * Shortens a name that is longer than [maxLength] by replacing its middle with an ellipsis, so
 * both its start and its end stay readable.
 *
 * For a file with a short extension the extension is kept whole and only the base name is
 * shortened, as the extension is often what tells two names apart. A name with no extension, a
 * dotfile, or an extension longer than [MAX_EXTENSION_LENGTH] characters is shortened as a whole.
 *
 * @param isFile    True for a file name, whose extension is preserved.
 * @param maxLength Length the result never exceeds.
 */
internal fun String.truncateMiddleKeepingExtension(isFile: Boolean, maxLength: Int = 40): String {
    if (length <= maxLength) return this
    val extension = if (isFile) substringAfterLast('.', "") else ""
    val baseName = substringBeforeLast('.', "")
    val keepsExtension = extension.isNotEmpty()
            && extension.length <= MAX_EXTENSION_LENGTH
            && baseName.isNotEmpty()
    return if (keepsExtension) {
        val suffix = ".$extension"
        baseName.truncateMiddle(maxLength - suffix.length) + suffix
    } else {
        truncateMiddle(maxLength)
    }
}

private fun String.truncateMiddle(maxLength: Int): String {
    if (length <= maxLength) return this
    val budget = (maxLength - ELLIPSIS.length).coerceAtLeast(0)
    val head = (budget + 1) / 2
    val tail = budget - head
    return take(head) + ELLIPSIS + takeLast(tail)
}
