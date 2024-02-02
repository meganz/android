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

    /**
     * Get the file preview download path
     *
     * @param fileName The name of the file
     */
    suspend fun getFilePreviewPath(fileName: String): String

    /**
     * Get the path to download file for preview
     */
    suspend fun getPreviewDownloadPathForNode(): String
}