package mega.privacy.android.domain.usecase.videosection

/**
 * Orders playlist titles the way a reader expects the list to read: case-insensitively, and
 * comparing runs of digits by value instead of character by character.
 *
 * The default name of a new playlist ends in a counter ("New playlist (2)", "New playlist (10)").
 * A plain lexicographic sort orders those as "(1), (10), (11), (2)", which reads as the list
 * having lost its sequence.
 *
 * Ties are broken by the raw comparison, so titles differing only in case or in leading zeros
 * still get a deterministic order.
 */
internal val playlistTitleComparator: Comparator<String> = Comparator { first, second ->
    val natural = compareNaturally(first, second)
    if (natural != 0) natural else first.compareTo(second)
}

private fun compareNaturally(first: String, second: String): Int {
    var firstIndex = 0
    var secondIndex = 0
    while (firstIndex < first.length && secondIndex < second.length) {
        val firstChar = first[firstIndex]
        val secondChar = second[secondIndex]
        if (firstChar.isDigit() && secondChar.isDigit()) {
            val firstStart = firstIndex
            val secondStart = secondIndex
            while (firstIndex < first.length && first[firstIndex].isDigit()) firstIndex++
            while (secondIndex < second.length && second[secondIndex].isDigit()) secondIndex++
            val firstDigits = first.substring(firstStart, firstIndex).trimStart('0')
            val secondDigits = second.substring(secondStart, secondIndex).trimStart('0')
            // Longer digit runs are larger, which also keeps counters that overflow Long comparable.
            val result = if (firstDigits.length != secondDigits.length) {
                firstDigits.length - secondDigits.length
            } else {
                firstDigits.compareTo(secondDigits)
            }
            if (result != 0) return result
        } else {
            val result = firstChar.lowercaseChar().compareTo(secondChar.lowercaseChar())
            if (result != 0) return result
            firstIndex++
            secondIndex++
        }
    }
    return (first.length - firstIndex) - (second.length - secondIndex)
}
