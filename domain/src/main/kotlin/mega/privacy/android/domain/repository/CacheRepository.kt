package mega.privacy.android.domain.repository

import java.io.File

/**
 * Cache Repository
 */
interface CacheRepository {
    /**
     * Get Cache Size
     */
    suspend fun getCacheSize(): Long

    /**
     * Clear Cache
     */
    suspend fun clearCache()

    /**
     * Get the [File] of the cache file
     *
     * @return the [File] of the cache file
     */
    fun getCacheFile(folderName: String, fileName: String): File?
}