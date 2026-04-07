package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ViewedLink

/**
 * Repository for managing viewed links — files and folders opened via MEGA deep links.
 *
 * Viewed links are stored in the `recently_used` table with type `file_link` or `folder_link`
 * and a non-null `link_url` column, which distinguishes them from regular "Continue Where You
 * Left Off" entries.
 */
interface ViewedLinksRepository {

    /**
     * Observes all viewed links (file and folder links), sorted by most recently accessed.
     *
     * @return a [Flow] emitting the current list of [ViewedLink] items whenever the data changes.
     */
    fun monitorLinks(): Flow<List<ViewedLink>>

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
    suspend fun removeLnk(nodeHandle: Long)

    /**
     * Deletes all viewed link entries (both file and folder links).
     */
    suspend fun clearLinks()
}