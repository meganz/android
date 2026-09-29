package mega.privacy.android.domain.repository.chat

import mega.privacy.android.domain.entity.chat.Giphy

/**
 * Repository for fetching GIFs from the Giphy API.
 */
interface GiphyRepository {

    /**
     * Get trending GIFs.
     *
     * @param limit Maximum number of GIFs to return.
     * @param offset Position of the first GIF to return.
     * @return List of [Giphy].
     */
    suspend fun getTrendingGiphys(limit: Int, offset: Int): List<Giphy>

    /**
     * Search GIFs.
     *
     * @param query Search term.
     * @param limit Maximum number of GIFs to return.
     * @param offset Position of the first GIF to return.
     * @return List of [Giphy].
     */
    suspend fun searchGiphys(query: String, limit: Int, offset: Int): List<Giphy>
}
