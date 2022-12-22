package mega.privacy.android.data.gateway

import mega.privacy.android.domain.entity.node.TypedFileNode
import java.io.File
import java.io.IOException

/**
 * File gateway
 *
 * @constructor Create empty File gateway
 */
interface FileGateway {
    /**
     * Get dir size
     *
     * @return total size of dir in bytes
     */
    suspend fun getDirSize(dir: File?): Long

    /**
     * Delete folder and subfolders
     *
     */
    @Throws(IOException::class)
    fun deleteFolderAndSubFolders(f: File?)

    /**
     * Is file available
     *
     * @param file
     * @return
     */
    suspend fun isFileAvailable(file: File?): Boolean

    /**
     * Build default download dir
     *
     */
    suspend fun buildDefaultDownloadDir(): File

    /**
     * Get the local folder path
     *
     * @param typedFileNode [TypedFileNode]
     * @return folder path or null
     */
    suspend fun getLocalFilePath(typedFileNode: TypedFileNode?): String?

    /**
     * Get offline files root path
     *
     * @return the root path of offline files
     */
    suspend fun getOfflineFilesRootPath(): String

    /**
     * Get offline files inbox root path
     *
     * @return the root path of inbox offline files
     */
    suspend fun getOfflineFilesInboxRootPath(): String
}
