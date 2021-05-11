package mega.privacy.android.app.textFileEditor

import kotlin.collections.ArrayList

class Pagination(text: String) {

    companion object {
        private const val CHARS_FOR_PAGE = 20000
    }

    private var pages: MutableList<String> = ArrayList()
    private var editedPages: MutableList<String> = ArrayList()
    private var currentPage = 0

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
            i = to + 1
        }

        if (i == 0) pages.add("")

        editedPages = pages
    }

    fun size(): Int = pages.size

    fun getCurrentPage(): Int = currentPage

    fun getCurrentPageText(): String = pages[currentPage]
}