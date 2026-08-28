package mega.privacy.android.feature.texteditor.components.markdown

/**
 * Pure parsing of list/quote/task markers at the start of a source line, backing the editor's
 * Enter-continuation and backspace-marker-removal typing behaviors. Kept free of Compose types
 * so the marker grammar is unit-testable in isolation.
 */
object MarkdownListContinuation {

    /**
     * A list/quote/task marker at the start of a line.
     *
     * @property length Characters from line start through the marker's trailing spaces.
     * @property continuation The marker to start the next line with (indentation preserved,
     * ordered numbers incremented, task boxes reset to unchecked).
     */
    data class Marker(val length: Int, val continuation: String)

    /** Parses the marker at the start of [line], or null when the line starts none. */
    fun markerOf(line: String): Marker? {
        var i = 0
        while (i < line.length && (line[i] == ' ' || line[i] == '\t')) i++
        val indent = line.substring(0, i)
        if (i >= line.length) return null
        val char = line[i]
        return when {
            char == '>' -> {
                var end = i + 1
                if (end < line.length && line[end] == ' ') end++
                Marker(end, "$indent> ")
            }

            char in "-*+" && i + 1 < line.length && line[i + 1] == ' ' -> {
                var end = i + 2
                while (end < line.length && line[end] == ' ') end++
                val taskEnd = taskBoxEnd(line, end)
                if (taskEnd != null) {
                    Marker(taskEnd, "$indent$char [ ] ")
                } else {
                    Marker(end, "$indent$char ")
                }
            }

            char.isDigit() -> {
                var digitsEnd = i
                while (digitsEnd < line.length && line[digitsEnd].isDigit()) digitsEnd++
                val hasSeparator = digitsEnd + 1 < line.length &&
                        (line[digitsEnd] == '.' || line[digitsEnd] == ')') &&
                        line[digitsEnd + 1] == ' '
                if (!hasSeparator) return null
                val number = line.substring(i, digitsEnd).toLongOrNull() ?: return null
                var end = digitsEnd + 2
                while (end < line.length && line[end] == ' ') end++
                val next = "$indent${number + 1}${line[digitsEnd]} "
                val taskEnd = taskBoxEnd(line, end)
                if (taskEnd != null) {
                    Marker(taskEnd, "$next[ ] ")
                } else {
                    Marker(end, next)
                }
            }

            else -> null
        }
    }

    /** True when [line] is nothing but its marker — an empty item that Enter should exit. */
    fun isEmptyItem(line: String): Boolean {
        val marker = markerOf(line) ?: return false
        return line.substring(marker.length).isBlank()
    }

    /** End of a GFM task box (`[ ] ` / `[x] `) starting at [from], or null. */
    private fun taskBoxEnd(line: String, from: Int): Int? {
        if (from + 3 >= line.length) return null
        val isBox = line[from] == '[' &&
                (line[from + 1] == ' ' || line[from + 1] == 'x' || line[from + 1] == 'X') &&
                line[from + 2] == ']' &&
                line[from + 3] == ' '
        if (!isBox) return null
        var end = from + 4
        while (end < line.length && line[end] == ' ') end++
        return end
    }
}
