package mega.privacy.android.data.gateway

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File
import java.io.IOException

/**
 * File gateway
 *
 * Refer to [mega.privacy.android.app.utils.FileUtil]
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
    suspend fun deleteFolderAndSubFolders(f: File?)

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
}