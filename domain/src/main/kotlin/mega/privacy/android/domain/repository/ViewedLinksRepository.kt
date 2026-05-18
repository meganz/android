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
     * Returns a [PagingSource] over all viewed links (file and folder links), sorted by
     * most recently accessed. The source is invalidated automatically whenever the
     * underlying table changes.
     */
    fun getViewedLinksPagingSource(): PagingSource<Int, ViewedLink>

    /**
     * Saves or updates a viewed link entry. If a link with the same node handle already exists,
     * its timestamp is updated (upsert behavior).
     *
     * @param viewedLink the [ViewedLink] to persist.
     */
    suspend fun saveLink(viewedLink: ViewedLink)

    /**
     * Removes a single viewed link entry by its node handle.
     *
     * @param nodeHandle the handle of the node to remove.
     */
    suspend fun removeLink(nodeHandle: Long)

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
