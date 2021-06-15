package mega.privacy.android.app.textFileEditor

import kotlin.collections.ArrayList

class Pagination(private val text: String, initialPage: Int) {

    companion object {
        private const val CHARS_FOR_PAGE = 20000
    }

    private var pages: MutableList<String?> = ArrayList()
    private var editedPages: MutableList<String?> = ArrayList()
    private var currentPage = initialPage

    init {
        var i = 0
        var to: Int
        var nextIndexOfReturn: Int

        while (i < text.length) {
            to = i + CHARS_FOR_PAGE
            nextIndexOfReturn = text.indexOf("\n", to)
            if (nextIndexOfReturn > to) to = nextIndexOfReturn
            if (to > text.length) to = text.length
            pages.add(text.substring(i, to))
            i = to
        }

        if (i == 0) pages.add("")

        editedPages = pages
    }

    fun size(): Int = pages.size

    fun isNotEmpty(): Boolean = text.isNotEmpty()

    fun getCurrentPage(): Int = currentPage

    fun getCurrentPageText(): String? = editedPages[currentPage]

    fun updatePage(text: String?) {
        editedPages[currentPage] = text
    }

    fun editionFinished() {
        pages = editedPages
    }

    fun isEdited(): Boolean = text != getEditedText()

    fun getEditedText(): String {
        var editedText = ""

        for (page in editedPages) {
            editedText += page
        }

        return editedText
    }

    fun previousPage() {
        if (currentPage - 1 >= 0) {
            currentPage--
        }
    }

    fun nextPage() {
        if (currentPage + 1 < pages.size) {
            currentPage++
        }
    }

    fun shouldShowPrevious(): Boolean = currentPage > 0

    fun shouldShowNext(): Boolean = pages.size > 1 && currentPage + 1 < pages.size
}