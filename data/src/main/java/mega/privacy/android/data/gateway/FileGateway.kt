package mega.privacy.android.data.gateway

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
     * Get local file
     *
     * @param fileName
     * @param fileSize
     * @param lastModifiedDate
     * @return local file if it exists
     */
    suspend fun getLocalFile(
        fileName: String,
        fileSize: Long,
        lastModifiedDate: Long,
    ): File?

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

    /**
     * remove GPS CoOrdinates from the file
     */
    suspend fun removeGPSCoordinates(filePath: String)

    /**
     * Copies a file from source to dest
     *
     * @param source Source file.
     * @param destination   Final copied file.
     * @throws IOException if some error happens while copying.
     */
    @Throws(IOException::class)
    suspend fun copyFile(source: File, destination: File)


    /**
     * creating a new temporary file in a root directory by copying the file from local path
     * to new path
     *
     * @param rootPath root path.
     * @param newPath new path of the file.
     * @param localPath  local path of the file.
     * @throws IOException if some error happens while creating.
     */
    @Throws(IOException::class)
    suspend fun createTempFile(rootPath: String, localPath: String, newPath: String)

    /**
     * check enough storage availability
     *
     * @param rootPath new Path of the file.
     * @param file file to be created.
     * @return [Boolean] whether enough storage available or not
     */
    suspend fun hasEnoughStorage(rootPath: String, file: File): Boolean

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
}
