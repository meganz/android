package mega.privacy.android.domain.repository

import java.io.File

/**
 * Cache Repository
 */
interface CacheRepository {
    /**
     * Get the cache size in bytes as reported by the system App info screen.
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
     * Get the folder [File] of the cache folder
     *
     * @return the folder [File] of the cache folder
     */
    suspend fun getCacheFolder(folderName: String): File?

    /**
     * Get the folder name for uploads
     * @param isForChat true if the folder is to upload chat files, false otherwise
     * @return the folder name to upload temporary files
     */
    fun getCacheFolderNameForTransfer(isForChat: Boolean): String

    /**
     * Get preview file, only if the cached copy still matches the current node.
     *
     * @param fileName The name of the file
     * @param fileSize The size of the node, used to discard an outdated cached copy
     * @param lastModifiedDate The modification time (in seconds) of the node, used to discard an outdated cached copy
     */
    suspend fun getPreviewFile(fileName: String, fileSize: Long, lastModifiedDate: Long): File?

    /**
     * Get the path to download file for preview
     */
    suspend fun getPreviewDownloadPathForNode(): String

    /**
     * @return true if the path represents a file or folder in the device cache directory, false otherwise
     */
    fun isFileInCacheDirectory(file: File): Boolean
}