package mega.privacy.android.domain.repository

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField

/**
 * Repository for managing viewed links — files and folders opened via MEGA deep links.
 *
 * Viewed links are stored in the `recently_used` table with type `file_link` or `folder_link`
 * and a non-null `link_url` column, which distinguishes them from regular "Continue Where You
 * Left Off" entries.
 */
interface ViewedLinksRepository {

    /**
     * Returns a [PagingSource] over all viewed links (file and folder links), sorted by the
     * supplied [sortField] and [sortDirection]. The source is invalidated automatically
     * whenever the underlying table changes.
     *
     * @param sortField the field used to order results.
     * @param sortDirection ascending or descending.
     */
    fun getViewedLinksPagingSource(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    ): PagingSource<Int, ViewedLink>

    /**
     * Saves or updates a viewed link entry. If a link with the same node handle already exists,
     * its timestamp is updated (upsert behavior).
     *
     * @param viewedLink the [ViewedLink] to persist.
     */
    suspend fun saveLink(viewedLink: ViewedLink)

    /**
     * Removes viewed link entries by their node handles.
     *
     * @param nodeHandles set of node handles to remove.
     */
    suspend fun removeLinks(nodeHandles: Set<Long>)

    /**
     * Deletes all viewed link entries (both file and folder links).
     */
    suspend fun clearLinks()

    /**
     * Observes the persisted sort preference for the viewed-links list.
     */
    fun monitorSortPreference(): Flow<Pair<ViewedLinksSortField, SortDirection>>

    /**
     * Persists the sort preference for the viewed-links list.
     */
    suspend fun setSortPreference(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    )
}
