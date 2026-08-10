package mega.privacy.android.feature.pdfviewer.presentation

import android.graphics.RectF
import com.shockwave.pdfium.PdfTextMatch
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource

/**
 * Wrapper for ByteArray to enable proper equality comparison in data classes.
 * ByteArray uses reference equality by default, but we need content-based equality.
 */
class PdfBytes(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is PdfBytes && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()
}

/**
 * Represents a single text search match within the PDF.
 *
 * @param pageIndex 0-based page index
 * @param charIndex character offset where match starts in the text page
 * @param charCount number of characters in the match
 */
data class SearchMatch(
    val pageIndex: Int,
    val charIndex: Int,
    val charCount: Int,
)

/**
 * State of the search functionality within the PDF viewer.
 *
 * @param query The current search query string
 * @param isSearchActive Whether the search mode (and search top bar) is active
 * @param results List of all matches across the document
 * @param currentMatchIndex Index of the currently highlighted match (-1 = none)
 * @param isSearching Whether a search is currently running in the ViewModel
 * @param currentMatchPdfRects PDF-coordinate RectFs for the current match highlight.
 *        These are in PDF point space and are converted to canvas coordinates at draw time.
 * @param currentMatchPageIndex 0-based page index of the current match (-1 = none)
 * @param allMatchRectsByPage PDF-coordinate RectFs for all matches, keyed by 0-based page index.
 *        Populated on demand as the user scrolls to each page and retained for the lifetime
 *        of the current search query. Cleared whenever the query changes.
 */
data class PdfViewerSearchState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val results: List<PdfTextMatch> = emptyList(),
    val currentMatchIndex: Int = -1,
    val isSearching: Boolean = false,
    val currentMatchPdfRects: List<RectF>? = null,
    val currentMatchPageIndex: Int = -1,
    val allMatchRectsByPage: Map<Int, List<RectF>> = emptyMap(),
) {
    val totalMatches: Int get() = results.size
    val hasResults: Boolean get() = results.isNotEmpty()

    val label = "${currentMatchIndex + 1}/${totalMatches}"
}

/**
 * UI state for the PDF Viewer screen.
 *
 * @param isLoading Whether the PDF is currently loading
 * @param isOnline Whether the device has an internet connection
 * @param source The source of the PDF document
 * @param title The title to display in the toolbar
 * @param currentPage The current page number (1-indexed). Null until the initial page is resolved
 * @param totalPages The total number of pages in the PDF
 * @param isToolbarVisible Whether the toolbar is currently visible
 * @param currentPassword The current password (if entered)
 * @param error The current error state, if any
 * @param isExternalFile Whether this is an external file (from intent)
 * @param searchState The state of the search functionality
 * @param isOffline Whether the file is from offline storage
 * @param isFromChat Whether the file is from chat
 * @param isFromFolderLink Whether the file is from a folder link
 * @param isFromFileLink Whether the file is from a file link
 * @param nodeHandle The handle of the node being viewed
 * @param nodeSourceType The source type of the node (for node options)
 * @param currentNode Resolved typed node for the PDF (null for external files or until fetched).
 *  The destination feeds this to NodeOptionsActionViewModel.updateSelectionModeAvailableActions
 *  to populate the floating-toolbar action list.
 */
internal data class PdfViewerState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val source: PdfViewerSource? = null,
    val title: String? = null,
    val currentPage: Int? = null,
    val totalPages: Int = 0,
    val isToolbarVisible: Boolean = true,
    val currentPassword: String? = null,
    val error: PdfViewerError? = null,
    val isExternalFile: Boolean = false,
    val isFileExplorerEnabled: Boolean = false,
    val searchState: PdfViewerSearchState = PdfViewerSearchState(),
    val isOffline: Boolean = false,
    val isFromChat: Boolean = false,
    val isFromFolderLink: Boolean = false,
    val isFromFileLink: Boolean = false,
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val currentNode: TypedNode? = null,
    val pdfBytes: PdfBytes? = null,
    val dismissEvent: StateEvent = consumed,
) {
    /**
     * Whether the PDF has been loaded successfully
     */
    val isLoaded: Boolean
        get() = !isLoading && error == null && source != null

    /**
     * Whether password protection error should be shown
     */
    val isPasswordError: Boolean
        get() = error is PdfViewerError.PasswordProtected || error is PdfViewerError.InvalidPassword
}
