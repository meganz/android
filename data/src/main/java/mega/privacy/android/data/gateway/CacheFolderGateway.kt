package mega.privacy.android.data.gateway

import nz.mega.sdk.MegaNode
import java.io.File

interface CacheFolderGateway {

    /**
     * Creates a Cache Folder if it does not exists and returns the file
     *
     * @param folderName Name of the cache folder
     */
    fun getCacheFolder(folderName: String): File?

    /**
     * Clears the public cache folder of app
     */
    fun clearPublicCache()

    /**
     * Creates single cache folder
     *
     * @param name Name of the file/folder
     */
    fun createCacheFolder(name: String)

    /**
     * Returns cache file if exists
     *
     * @param folderName Name of the parent folder
     * @param fileName Name of the file
     */
    fun getCacheFile(folderName: String, fileName: String?): File?

    /**
     * Calculates and returns the cache size for the app files in bytes
     */
    fun getCacheSize(): Long

    /**
     * Clears the cache folder of app
     */
    suspend fun clearCache()

    /**
     *  Deletes the Cache folder if it is empty
     *
     *  @param folderName Name of the folder
     */
    fun deleteCacheFolderIfEmpty(folderName: String)

    /**
     * Removes old temporary folder if it exists
     *
     * @param folderName name of the folder
     */
    fun removeOldTempFolder(folderName: String)

    /**
     * Returns the requested file with the given name
     *
     * @param folderName Name of the folder
     */
    fun getOldTempFolder(folderName: String): File

    /**
     * return the avatar file
     *
     * @param fileName name of the file
     */
    fun buildAvatarFile(fileName: String?): File?

    /**
     * create a default download location file
     */
    suspend fun buildDefaultDownloadDir(): File

    /**
     * Return node thumbnail folder
     */
    fun getThumbnailCacheFolder(): File?

    /**
     * Return node preview folder
     */
    fun getPreviewCacheFolder(): File?

    /**
     * Return node full size folder
     */
    fun getFullSizeCacheFolder(): File?

    /**
     * Removes old temp folders.
     */
    suspend fun removeOldTempFolders()

    /**
     * Removes app data.
     */
    suspend fun clearAppData()

    /**
     * Returns cache directory of the app
     */
    val cacheDir: File
}
