package mega.privacy.android.domain.repository

import java.io.File

/**
 * The repository interface regarding thumbnail feature.
 */
interface ThumbnailRepository {

    /**
     * Check thumbnail from local
     * @param handle node handle
     * @return thumbnail file
     */
    suspend fun getThumbnailFromLocal(handle: Long): File?

    /**
     * Check thumbnail from server
     * @param handle node handle
     * @return thumbnail file
     */
    suspend fun getThumbnailFromServer(handle: Long): File?
}