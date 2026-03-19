package mega.privacy.android.feature.pdfviewer.search

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfTextMatch
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineImpl.Companion.FLAG_CONSECUTIVE
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineImpl.Companion.FLAG_MATCH_CASE
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineImpl.Companion.FLAG_MATCH_WHOLE_WORD
import timber.log.Timber
import java.io.File

/**
 * Search engine for PDF text search using PdfiumCore.
 *
 * Owns its own [PdfDocument] instance, independent of the rendering [PDFView].
 * This ensures thread-safety: the rendering thread and search coroutines never
 * touch the same native pointers simultaneously.
 *
 * Usage:
 * 1. Call [openFromBytes] or [openFromUri] to open the document
 * 2. Call [searchAllPages] to find matches
 * 3. Call [getPdfRects] to get bounding rectangles for a match
 * 4. Call [close] when done to release native resources
 *
 * @param context Android context (used for PdfiumCore initialisation)
 * @param pdfiumCore PdfiumCore instance (injectable for testing)
 * @param dispatcher Coroutine dispatcher for search operations (injectable for testing)
 */
internal class PdfSearchEngineImpl(
    private val context: Context,
    private val pdfiumCore: PdfiumCore = PdfiumCore(context),
    private val dispatcher: CoroutineDispatcher,
) : PdfSearchEngine {

    private var pdfDocument: PdfDocument? = null

    /**
     * Open a PDF from a byte array.
     *
     * @param bytes The raw PDF bytes
     * @param password Optional password for encrypted PDFs
     * @return true if the document was opened successfully
     */
    override fun openFromBytes(bytes: ByteArray, password: String?): Boolean {
        closeDocument()
        return runCatching {
            pdfDocument = pdfiumCore.newDocument(bytes, password)
            true
        }.onFailure { Timber.e(it, "PdfSearchEngine: failed to open from bytes") }
            .getOrDefault(false)
    }

    /**
     * Open a PDF from an Android Uri via ContentResolver.
     *
     * @param uri The URI to open (content:// or file://)
     * @param password Optional password for encrypted PDFs
     * @return true if the document was opened successfully
     */
    override fun openFromUri(uri: Uri, password: String?): Boolean {
        closeDocument()
        return runCatching {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return false
            pfd.use { descriptor ->
                pdfDocument = pdfiumCore.newDocument(descriptor, password)
            }
            true
        }.onFailure { Timber.e(it, "PdfSearchEngine: failed to open from URI $uri") }
            .getOrDefault(false)
    }

    /**
     * Open a PDF from a local file path.
     * This is more reliable than [openFromUri] for local files, especially on Android 10+
     * where ContentResolver may not handle file:// URIs properly.
     *
     * @param filePath The absolute path to the local PDF file
     * @param password Optional password for encrypted PDFs
     * @return true if the document was opened successfully
     */
    override fun openFromFilePath(filePath: String, password: String?): Boolean {
        closeDocument()
        return runCatching {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.w("PdfSearchEngine: file does not exist: $filePath")
                return false
            }
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd.use { pdfDocument = pdfiumCore.newDocument(it, password) }
            true
        }.onFailure { Timber.e(it, "PdfSearchEngine: failed to open from file path $filePath") }
            .getOrDefault(false)
    }

    /**
     * Search all pages in the document for the given query.
     *
     * Searches starting from [startPage] and wraps around, so the first result
     * is always the closest one to the currently visible page.
     * Only [FLAG_MATCH_CASE] is currently applied; [FLAG_MATCH_WHOLE_WORD] and [FLAG_CONSECUTIVE]
     * are reserved for future use (native API support may vary).
     *
     * @param query The search query string (queries with fewer than 2 characters return no results)
     * @param startPage The 0-based page index to start searching from
     * @param flags Search flags: [FLAG_MATCH_CASE] is applied; others reserved
     * @return List of [PdfTextMatch] objects, ordered starting from [startPage]
     */
    override suspend fun searchAllPages(
        query: String,
        startPage: Int,
        flags: Int,
    ): List<PdfTextMatch> = withContext(dispatcher) {
        val doc = pdfDocument ?: return@withContext emptyList()
        val totalPages = pdfiumCore.getPageCount(doc)
        if (shouldSkipSearch(totalPages, query)) {
            return@withContext emptyList()
        }

        val trimmed = query.trim()
        val results = mutableListOf<PdfTextMatch>()
        val pageOrder = pageOrderForSearch(totalPages, startPage)

        for (pageIndex in pageOrder) {
            ensureActive()
            try {
                pdfiumCore.openPage(doc, pageIndex)
                try {
                    val matches = searchPage(doc.docPtr, pageIndex, trimmed, flags)
                    results.addAll(matches)
                } finally {
                    pdfiumCore.closeTextPage(doc, pageIndex)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "PdfSearchEngine: error searching page $pageIndex")
            }
        }
        results
    }

    private suspend fun searchPage(
        docPtr: Long,
        pageIndex: Int,
        query: String,
        flags: Int,
    ): List<PdfTextMatch> {
        currentCoroutineContext().ensureActive()
        return try {
            val matchCase = (flags and FLAG_MATCH_CASE) == FLAG_MATCH_CASE
            pdfiumCore.searchOnePage(docPtr, pageIndex, query, matchCase)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "PdfSearchEngine: searchOnePage failed for page $pageIndex")
            emptyList()
        }
    }

    /**
     * Get bounding rectangles for a search match in PDF coordinates.
     *
     * These PDF-coordinate rects must be converted to canvas coordinates at
     * draw time using [PDFView.mapRectToCanvas] because the canvas scale depends
     * on zoom level, which changes dynamically.
     *
     * @param match The search match to retrieve rects for
     * @return List of [RectF] in PDF coordinate space (points, 1/72", origin at bottom-left)
     */
    override fun getPdfRects(match: PdfTextMatch): List<RectF> {
        val doc = pdfDocument ?: run {
            Timber.w("PdfSearchEngine: getPdfRects called but document is null")
            return emptyList()
        }
        return runCatching {
            val textPagePtr = pdfiumCore.openTextPage(doc, match.pageIndex)
            try {
                pdfiumCore.getMatchRects(textPagePtr, match.charIndex, match.charCount)
            } finally {
                pdfiumCore.closeTextPage(doc, match.pageIndex)
            }
        }.onFailure { Timber.e(it, "PdfSearchEngine: failed to get rects for match") }
            .getOrDefault(emptyList())
    }

    /**
     * Returns true if the document has been successfully opened.
     */
    override val isOpen: Boolean get() = pdfDocument != null

    private fun closeDocument() {
        runCatching {
            pdfDocument?.let { pdfiumCore.closeDocument(it) }
        }
        pdfDocument = null
    }

    override fun close() {
        closeDocument()
    }

    companion object {
        /** Minimum number of characters in a search query (shorter queries return no results). */
        const val MIN_QUERY_LENGTH = 2

        const val FLAG_MATCH_CASE = PdfiumCore.FLAG_MATCH_CASE
        const val FLAG_MATCH_WHOLE_WORD = PdfiumCore.FLAG_MATCH_WHOLE_WORD
        const val FLAG_CONSECUTIVE = PdfiumCore.FLAG_CONSECUTIVE

        /**
         * Returns true if search should be skipped due to invalid query or document state.
         *
         * @param totalPages Total number of pages in the document
         * @param query The search query (will be trimmed)
         * @return true if search should return empty results early
         */
        internal fun shouldSkipSearch(totalPages: Int, query: String): Boolean {
            val trimmed = query.trim()
            return totalPages == 0 || trimmed.isEmpty() || trimmed.length < MIN_QUERY_LENGTH
        }

        /**
         * Returns the 0-based page indices in search order: from [startPage] to end, then from 0 to startPage-1.
         * Handles startPage >= totalPages by returning indices 0 until totalPages.
         */
        internal fun pageOrderForSearch(totalPages: Int, startPage: Int): List<Int> {
            if (totalPages <= 0) return emptyList()
            val start = startPage.coerceIn(0, totalPages - 1)
            return (start until totalPages) + (0 until start)
        }
    }
}
