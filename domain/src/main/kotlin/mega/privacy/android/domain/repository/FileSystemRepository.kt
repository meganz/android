package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.ViewerNode
import java.io.File
import java.io.IOException

/**
 * File System repository
 *
 */
interface FileSystemRepository {
    /**
     * Get offline path
     *
     * @return root path for offline files
     */
    suspend fun getOfflinePath(): String

    /**
     * Get offline inbox path
     *
     * @return offline files inbox path
     */
    suspend fun getOfflineInboxPath(): String

    /**
     * Create Folder
     */
    suspend fun createFolder(name: String): Long?

    /**
     * Downloads a file node in background.
     *
     * @param viewerNode File node to download.
     * @return The local path of the downloaded file.
     */
    suspend fun downloadBackgroundFile(viewerNode: ViewerNode): String

    /**
     * setMyChatFilesFolder
     * @param nodeHandle
     * @return node handle [Long]
     */
    suspend fun setMyChatFilesFolder(nodeHandle: Long): Long?

    /**
     * Get file versions option
     *
     * @param forceRefresh
     * @return
     */
    suspend fun getFileVersionsOption(forceRefresh: Boolean): Boolean

    /**
     * Get local file
     *
     * @param fileNode
     * @return local file if it exists
     */
    suspend fun getLocalFile(fileNode: FileNode): File?

    /**
     * Get file streaming uri for a node
     *
     * @param node
     * @return local url string if found
     */
    suspend fun getFileStreamingUri(node: Node): String?

    /**
     * create temp file in file system
     * @param root root path
     * @param syncRecord
     */
    @Throws(IOException::class)
    suspend fun createTempFile(root: String, syncRecord: SyncRecord): String?

    /**
     * remove GPS CoOrdinates from the file
     */
    suspend fun removeGPSCoordinates(filePath: String)


    /**
     * Get disk space
     *
     * @param path
     * @return disk space at path in bytes
     */
    suspend fun getDiskSpaceBytes(path: String): Long

    /**
     * Delete File
     *
     * @param file
     * @return [Boolean]
     */
    suspend fun deleteFile(file: File): Boolean

    /**
     * create directory on a specified path
     * @param path
     * @return [Boolean]
     */
    suspend fun createDirectory(path: String): File

    /**
     * remove directory recursively
     * @param path
     * @return [Boolean]
     */
    suspend fun deleteDirectory(path: String): Boolean

    /**
     * Returns cache directory of the app
     */
    val cacheDir: File
}
