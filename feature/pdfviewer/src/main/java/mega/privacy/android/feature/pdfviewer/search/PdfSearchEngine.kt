package mega.privacy.android.feature.pdfviewer.search

import android.graphics.RectF
import android.net.Uri
import com.shockwave.pdfium.PdfTextMatch

/**
 * Abstraction for PDF text search operations.
 *
 * Allows the ViewModel to be unit-tested with a fake implementation that returns
 * controlled results without depending on PdfiumCore or Android runtime.
 */
internal interface PdfSearchEngine : AutoCloseable {

    /**
     * Open a PDF from a byte array.
     *
     * @param bytes The raw PDF bytes
     * @param password Optional password for encrypted PDFs
     * @return true if the document was opened successfully
     */
    fun openFromBytes(bytes: ByteArray, password: String? = null): Boolean

    /**
     * Open a PDF from an Android Uri via ContentResolver.
     *
     * @param uri The URI to open (content:// or file://)
     * @param password Optional password for encrypted PDFs
     * @return true if the document was opened successfully
     */
    fun openFromUri(uri: Uri, password: String? = null): Boolean

    /**
     * Open a PDF from a local file path.
     *
     * @param filePath The absolute path to the local PDF file
     * @param password Optional password for encrypted PDFs
     * @return true if the document was opened successfully
     */
    fun openFromFilePath(filePath: String, password: String? = null): Boolean

    /**
     * Search all pages in the document for the given query.
     *
     * @param query The search query string
     * @param startPage The 0-based page index to start searching from
     * @param flags Search flags (e.g. [PdfSearchEngineImpl.FLAG_MATCH_CASE])
     * @return List of [PdfTextMatch] objects, ordered starting from [startPage]
     */
    suspend fun searchAllPages(
        query: String,
        startPage: Int = 0,
        flags: Int = 0,
    ): List<PdfTextMatch>

    /**
     * Get bounding rectangles for a search match in PDF coordinates.
     *
     * @param match The search match to retrieve rects for
     * @return List of [RectF] in PDF coordinate space
     */
    fun getPdfRects(match: PdfTextMatch): List<RectF>

    /** Returns true if the document has been successfully opened. */
    val isOpen: Boolean
}
