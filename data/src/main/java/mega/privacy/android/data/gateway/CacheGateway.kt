package mega.privacy.android.data.gateway

import java.io.File

/**
 * Cache folder gateway
 */
interface CacheGateway {

    /**
     * Get a folder in cache and returns the file
     * Create the folder if it does not exist
     *
     * @param folderName
     * @return the File corresponding to the folder in cache
     *         Return null if the folder cannot be created
     */
    suspend fun getOrCreateCacheFolder(folderName: String): File?

    /**
     * Get the chat folder in cache and returns the file
     * Create the folder if it does not exist
     *
     * @return the File corresponding to the chat folder in cache
     *         Return null if the folder cannot be created
     */
    suspend fun getOrCreateChatCacheFolder(): File?

    /**
     * Get an instance of a file in cache
     * The file does not necessarily exist in cache
     *
     * @param folderName the name of the parent folder
     * @param fileName the name of the file
     * @return the File associated to the fileName and folderName in cache,
     *         Return null if the folder cannot be created
     */
    suspend fun getCacheFile(folderName: String, fileName: String): File?


    /**
     * Remove the contents of the internal cache directory
     */
    suspend fun clearCacheDirectory()


    /**
     * Return node thumbnail folder
     */
    suspend fun getThumbnailCacheFolder(): File?

    /**
     * Return node preview folder
     */
    suspend fun getPreviewCacheFolder(): File?

    /**
     * Return node full size folder
     */
    suspend fun getFullSizeCacheFolder(): File?


    /**
     * Return Camera Uploads Cache Folder
     */
    suspend fun getCameraUploadsCacheFolder(): File?

    /**
     * return the avatar file
     *
     * @param fileName name of the file
     */
    suspend fun buildAvatarFile(fileName: String?): File?

    /**
     * Removes content of internal files directory
     */
    suspend fun clearAppData()

    /**
     * clear sdk cache
     *
     * clears cache files in mega app directory
     */
    suspend fun clearSdkCache()

}