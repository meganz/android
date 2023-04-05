package mega.privacy.android.data.gateway

import android.net.Uri

/**
 * Gateway class that provides SD Card-related implementations
 */
interface SDCardGateway {

    /**
     * Retrieves the Folder Directory name in the SD Card
     *
     * @see mega.privacy.android.app.utils.SDCardUtils.getSDCardDirName
     * @param uri The [Uri] to get the name of the folder
     *
     * @return The Folder Directory name
     */
    suspend fun getDirectoryName(uri: Uri): String

    /**
     * Checks whether a given Folder is inside the SD Card
     *
     * @see mega.privacy.android.app.utils.SDCardUtils.isLocalFolderOnSDCard
     * @param localPath The Folder local path
     *
     * @return true if the Folder exists, and false if otherwise
     */
    suspend fun doesFolderExists(localPath: String): Boolean

    /**
     * Retrieves the Root SD Card path
     *
     * @see mega.privacy.android.app.utils.SDCardUtils.getSDCardRoot
     * @param path the File path
     *
     * @return the Root SD Card path
     */
    suspend fun getRootSDCardPath(path: String): String
}