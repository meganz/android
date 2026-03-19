package mega.privacy.android.feature.pdfviewer.search

import android.graphics.RectF
import android.net.Uri
import com.shockwave.pdfium.PdfTextMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake [PdfSearchEngine] for unit testing the search pipeline in [PdfViewerViewModel].
 *
 * Configure [searchResults] and [getPdfRectsResult] to control what the ViewModel receives.
 */
class FakePdfSearchEngine : PdfSearchEngine {

    private val _isOpen = MutableStateFlow(false)
    val isOpenFlow: StateFlow<Boolean> = _isOpen.asStateFlow()

    override val isOpen: Boolean get() = _isOpen.value

    /** Results to return from [searchAllPages]. Default: empty list. */
    var searchResults: List<PdfTextMatch> = emptyList()

    /** Results to return from [getPdfRects]. Default: empty list. */
    var getPdfRectsResult: List<RectF> = emptyList()

    override fun openFromBytes(bytes: ByteArray, password: String?): Boolean {
        _isOpen.value = true
        return true
    }

    override fun openFromUri(uri: Uri, password: String?): Boolean {
        _isOpen.value = true
        return true
    }

    override fun openFromFilePath(filePath: String, password: String?): Boolean {
        _isOpen.value = true
        return true
    }

    override suspend fun searchAllPages(
        query: String,
        startPage: Int,
        flags: Int,
    ): List<PdfTextMatch> = searchResults

    override fun getPdfRects(match: PdfTextMatch): List<RectF> = getPdfRectsResult

    override fun close() {
        _isOpen.value = false
    }
}
