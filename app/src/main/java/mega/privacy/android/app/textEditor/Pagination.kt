package mega.privacy.android.app.textEditor

import java.util.regex.Pattern
import kotlin.collections.ArrayList

class Pagination(private var text: String, initialPage: Int) {

    companion object {
        private const val CHARS_FOR_PAGE = 30000
        const val LINE_BREAK = "\n"
    }

    private var pages: MutableList<String?> = ArrayList()
    private var currentPage = initialPage
    private var firstLineNumber = 1

    init {
        var i = 0
        var to: Int
        var nextIndexOfReturn: Int

        while (i < text.length) {
            to = i + CHARS_FOR_PAGE
            nextIndexOfReturn = text.indexOf(LINE_BREAK, to)

            if (nextIndexOfReturn > to) {
                to = nextIndexOfReturn + 1
            }

            if (to > text.length) {
                to = text.length
            }

            pages.add(text.substring(i, to))
            i = to
        }

        if (i == 0) pages.add("")
    }

    fun size(): Int = pages.size

    fun isNotEmpty(): Boolean = text.isNotEmpty()

    fun getCurrentPage(): Int = currentPage

    fun getCurrentPageText(): String? = pages[currentPage]

    fun updatePage(text: String?) {
        pages[currentPage] = text
    }

    fun editionFinished() {
        text = getEditedText()
    }

    fun isEdited(): Boolean = text != getEditedText()

    fun getEditedText(): String {
        var editedText = ""

        for (page in pages) {
            editedText += page
        }

        return editedText
    }

    fun previousPage() {
        if (currentPage - 1 >= 0) {
            currentPage--
            updateFirstLineNumber()
        }
    }

    fun nextPage() {
        if (currentPage + 1 < pages.size) {
            currentPage++
            updateFirstLineNumber()
        }
    }

    /**
     * Updates the value to show as first line number of the page.
     */
    private fun updateFirstLineNumber() {
        if (currentPage == 0) {
            firstLineNumber = 1
            return
        }

        var firstLine = 1

        for (page in 0 until currentPage) {
            val text = pages[page]
            firstLine += getNumberOfLines(text)
        }

        firstLineNumber = firstLine
    }

    /**
     * Gets the number of lines of a text.
     *
     * @param text Text to get its number of lines.
     * @return The number of lines.
     */
    private fun getNumberOfLines(text: String?): Int {
        if (text == null) {
            return 0
        }

        val matcher = Pattern.compile(LINE_BREAK).matcher(text)
        var count = 0

        while (matcher.find()) {
            count++
        }

        return count
    }

    fun getFirstLineNumber(): Int = firstLineNumber

    fun shouldShowPrevious(): Boolean = currentPage > 0

    fun shouldShowNext(): Boolean = pages.size > 1 && currentPage + 1 < pages.size
}