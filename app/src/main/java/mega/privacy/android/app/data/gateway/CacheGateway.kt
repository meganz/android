package mega.privacy.android.app.data.gateway

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
}