package mega.privacy.android.feature.documentscanner.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession

/**
 * Repository for managing a document scanning session.
 */
interface ScanSessionRepository {

    /**
     * Observes the current scan session.
     *
     * @return a [Flow] emitting the current [ScanSession] on every change
     */
    fun getSession(): Flow<ScanSession>

    /**
     * Adds a page to the current session.
     *
     * @param page the [ScannedPage] to add
     */
    suspend fun addPage(page: ScannedPage)

    /**
     * Removes a page from the current session by its ID.
     *
     * @param pageId the unique identifier of the page to remove
     */
    suspend fun removePage(pageId: String)

    /**
     * Reorders a page within the current session.
     *
     * @param fromIndex the current position of the page
     * @param toIndex the target position of the page
     */
    suspend fun reorderPages(fromIndex: Int, toIndex: Int)

    /**
     * Replaces a page in the current session (e.g., after retake).
     *
     * @param pageId the unique identifier of the page to replace
     * @param newPage the replacement [ScannedPage]
     */
    suspend fun replacePage(pageId: String, newPage: ScannedPage)

    /**
     * Clears all pages and resets the session.
     */
    suspend fun clearSession()

    /**
     * Returns all pages in the current session.
     *
     * @return the list of [ScannedPage] in order
     */
    suspend fun getPages(): List<ScannedPage>
}
