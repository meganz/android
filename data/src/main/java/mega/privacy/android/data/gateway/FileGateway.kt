package mega.privacy.android.data.gateway

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
}