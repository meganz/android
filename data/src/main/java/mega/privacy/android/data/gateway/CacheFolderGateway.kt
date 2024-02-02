package mega.privacy.android.data.gateway

import java.io.File

/**
 * Gateway class for caching operations
 */
interface CacheFolderGateway {

    /**
     * Creates a Cache Folder if it does not exists and returns the file
     *
     * @param folderName Name of the cache folder
     */
    fun getCacheFolder(folderName: String): File?

    /**
     * Creates a Cache Folder if it does not exists and returns the file
     *
     * @param folderName Name of the cache folder
     */
    suspend fun getCacheFolderAsync(folderName: String): File?

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
     * Returns a [File] reference in the cache folder and given subfolder and with the given file name, even if it does not exist.
     * If the folder does not exist it will be created.
     * If the file does not exist a [File] instance will be returned either but not created, so it can be used as path reference.
     * see [File.exists] to check whether the file exists or not if needed.
     *
     * @param folderName Name of the parent folder (a subfolder of cache folder)
     * @param fileName Name of the file
     * @return [File] pointing to the specified path. In case of an error creating the subfolder, if needed, or null [fileName], null will be returned
     */
    fun getCacheFile(folderName: String, fileName: String?): File?

    /**
     * Returns a [File] reference in the cache folder and given subfolder and with the given file name, even if it does not exist.
     * If the folder does not exist it will be created.
     * If the file does not exist a [File] instance will be returned either but not created, so it can be used as path reference.
     * see [File.exists] to check whether the file exists or not if needed.
     *
     * @param folderName Name of the parent folder (a subfolder of cache folder)
     * @param fileName Name of the file
     * @return [File] pointing to the specified path. In case of an error creating the subfolder, if needed, or null [fileName], null will be returned
     */
    suspend fun getCacheFileAsync(folderName: String, fileName: String?): File?

    /**
     * Calculates and returns the cache size for the app files in bytes
     */
    suspend fun getCacheSize(): Long

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
     * create a default download location file
     */
    suspend fun buildDefaultDownloadDir(): File

    /**
     * Get the path to download file for preview
     */
    suspend fun getPreviewDownloadPathForNode(): String

    /**
     * Get the path of the file preview download
     *
     * @param fileName Name of the file
     */
    suspend fun getFilePreviewPath(fileName: String): String
}
