package mega.privacy.android.shared.original.core.ui.utils.textcomparator

import java.math.BigDecimal

/**
 * Comparator to sort strings in alphanumeric order.
 */
class AlphanumericComparator : Comparator<String> {

    /**
     * Compares two strings in alphanumeric order.
     *
     * @param str1 First string to compare.
     * @param str2 Second string to compare.
     * @return A negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     */
    override fun compare(str1: String, str2: String): Int {
        var i = 0
        var j = 0

        while (i < str1.length && j < str2.length) {
            val char1 = str1[i]
            val char2 = str2[j]

            if (char1.isDigit() && char2.isDigit()) {
                // Extract the full number from both strings
                val num1 = extractNumber(str1, i)
                val num2 = extractNumber(str2, j)

                if (num1 != num2) {
                    return num1.compareTo(num2)
                }

                // Move indices past the number
                i += num1.toString().length
                j += num2.toString().length
            } else {
                if (char1 != char2) {
                    return char1.compareTo(char2)
                }

                i++
                j++
            }
        }

        // If one string is a prefix of the other, the shorter string comes first
        return str1.length.compareTo(str2.length)
    }


    private fun extractNumber(str: String, startIndex: Int): BigDecimal {
        var endIndex = startIndex
        while (endIndex < str.length && str[endIndex].isDigit()) {
            endIndex++
        }
        return runCatching { str.substring(startIndex, endIndex).toBigDecimal() }
            .getOrDefault(BigDecimal.ZERO)
    }
}
