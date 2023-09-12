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
     * The local DCIM Folder path
     */
    val localDCIMFolderPath: String

    /**
     * Get offline path
     *
     * @return root path for offline files
     */
    suspend fun getOfflinePath(): String

    /**
     * Get offline Backups path
     *
     * @return offline files Backups path
     */
    suspend fun getOfflineBackupsPath(): String

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
    suspend fun createTempFile(root: String, syncRecord: SyncRecord): String

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
     * Creates the temporary Camera Uploads root directory
     */
    suspend fun createCameraUploadTemporaryRootDirectory(): File?

    /**
     * Recursively deletes the temporary Camera Uploads root directory
     *
     * @return true if the delete operation is successful, and false if otherwise
     */
    suspend fun deleteCameraUploadsTemporaryRootDirectory(): Boolean

    /**
     * Get the fingerprint of a file by path
     *
     * @param filePath file path
     * @return fingerprint
     */
    suspend fun getFingerprint(filePath: String): String?

    /**
     * Checks whether the Folder exists
     *
     * @param folderPath The Folder path
     *
     * @return true if the Folder exists, and false if otherwise
     */
    suspend fun doesFolderExists(folderPath: String): Boolean

    /**
     * Checks for the Folder existence in the SD Card
     *
     * @param uriString The Folder path in the SD Card
     *
     * @return true if it exists, and false if otherwise
     */
    suspend fun isFolderInSDCardAvailable(uriString: String): Boolean

    /**
     * Checks whether the External Storage Directory exists
     *
     * @return true if it exists, and false if otherwise
     */
    suspend fun doesExternalStorageDirectoryExists(): Boolean

    /**
     * Does file exist
     *
     * @param path
     * @return true if file exists, else false
     */
    suspend fun doesFileExist(path: String): Boolean

    /**
     * Returns the parent path of the file represented by path
     */
    suspend fun getParent(path: String): String

    /**
     * Update media store by scanning specified files with corresponding mime types.
     */
    fun scanMediaFile(paths: Array<String>, mimeTypes: Array<String>)

    /**
     * Returns an external storage path based on content Uri
     */
    suspend fun getExternalPathByContentUri(uri: String): String?

    /**
     * Tries to determine the content type of an object,
     * based on the specified "file" component of a URL.
     *
     * @param localPath Local path of the file
     * @return The content type of an object if any.
     */
    suspend fun getGuessContentTypeFromName(localPath: String): String?

    /**
     * Get GPS coordinates from video file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getVideoGPSCoordinates(filePath: String): Pair<Float, Float>

    /**
     * Get GPS coordinates from photo file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Float, Float>

    /**
     * Make a name suitable for a file name in the local filesystem
     *
     * This function escapes (%xx) forbidden characters in the local filesystem if needed.
     * You can revert this operation using MegaApi::unescapeFsIncompatible
     *
     * If no dstPath is provided or filesystem type it's not supported this method will
     * escape characters contained in the following list: \/:?\"<>|*
     * Otherwise it will check forbidden characters for local filesystem type
     *
     * The input string must be UTF8 encoded. The returned value will be UTF8 too.
     *
     * You take the ownership of the returned value
     *
     * @param fileName Name to convert (UTF8)
     * @param dstPath  Destination path
     * @return Converted name (UTF8)
     */
    suspend fun escapeFsIncompatible(fileName: String, dstPath: String): String?
}
