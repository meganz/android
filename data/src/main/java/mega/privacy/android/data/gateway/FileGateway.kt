package mega.privacy.android.data.gateway

import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

/**
 * File gateway
 *
 * @constructor Create empty File gateway
 */
interface FileGateway {

    /**
     * The local DCIM Folder path
     */
    val localDCIMFolderPath: String

    /**
     * Get dir size
     *
     * @return total size of dir in bytes
     */
    suspend fun getDirSize(dir: File?): Long

    /**
     * Delete folder and sub folders
     *
     */
    fun deleteFolderAndSubFolders(folder: File?): Boolean

    /**
     * Is file available
     *
     * @param file
     * @return
     */
    suspend fun isFileAvailable(file: File?): Boolean

    /**
     * Checks whether the passed [DocumentFile] is available or not
     *
     * @param documentFile A potentially nullable [DocumentFile]
     *
     * @return true if the [DocumentFile] is available, and false if otherwise
     */
    suspend fun isDocumentFileAvailable(documentFile: DocumentFile?): Boolean

    /**
     * Checks whether the External Storage Directory exists
     *
     * @return true if it exists, and false if otherwise
     */
    suspend fun doesExternalStorageDirectoryExists(): Boolean

    /**
     * Checks whether the [File] is available or not from the given [String] path
     *
     * @param fileString The [String] containing the file location
     *
     * @return true if the file exists, and false if otherwise
     */
    suspend fun isFileAvailable(fileString: String): Boolean

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

    /**
     * Scan media file to refresh media store
     * @param paths Array of paths to be scanned.
     * @param mimeTypes Optional array of MIME types for each path.
     */
    fun scanMediaFile(paths: Array<String>, mimeTypes: Array<String>)


    /**
     * Takes a content Uri and creates an external storage path from it
     *
     * e.g. the following Uri:
     * "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
     * will be converted to
     * "/storage/emulated/0/Sync/someFolder"
     *
     * @param contentUri The content Uri to be converted
     */
    suspend fun getExternalPathByContentUri(contentUri: String): String?

    /**
     * delete files in a directory
     *
     * @param directory the directory from which files are deleted
     * lists files in the directory and remove only files from the directory not folders
     */
    suspend fun deleteFilesInDirectory(directory: File)

    /**
     * Build external storage file
     *
     * @param filePath    Path of the file
     * @return            The external storage [File]
     */
    suspend fun buildExternalStorageFile(filePath: String): File

    /**
     * Rename file
     *
     * @param oldFile    [File] to be renamed
     * @param newName    New name for the file
     * @return           True if success or false otherwise
     */
    suspend fun renameFile(oldFile: File, newName: String): Boolean

    /**
     * get absolute path of a file if exists
     *
     * @param path
     */
    suspend fun getAbsolutePath(path: String): String?
}
