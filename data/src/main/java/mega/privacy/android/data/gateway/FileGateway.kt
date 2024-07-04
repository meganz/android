package mega.privacy.android.data.gateway

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.uri.UriPath
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
     * @return total size of file or dir in bytes
     */
    suspend fun getTotalSize(file: File?): Long

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
     * Get file by path if it exists
     */
    suspend fun getFileByPath(path: String): File?

    /**
     * Get offline files root path
     *
     * @return the root path of offline files
     */
    suspend fun getOfflineFilesRootPath(): String

    /**
     * Get offline files Backups root path
     *
     * @return the root path of Backups offline files
     */
    suspend fun getOfflineFilesBackupsRootPath(): String

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
     * Copy files allow to copy files from source folder to destination
     *
     * @param source
     * @param destination
     */
    suspend fun copyFileToFolder(source: File, destination: File): Int

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

    /**
     * Sets the last-modified time of the file or directory named by this abstract pathname
     *
     * @param path
     * @param timestamp
     * @return true if and only if the operation succeeded; false otherwise, null if the file does not exist
     */
    suspend fun setLastModified(path: String, timestamp: Long): Boolean?

    /**
     * Saves the text in the given content uri
     *
     * @param uri content uri to be written in
     * @param text Text to write in the content uri
     */
    suspend fun saveTextOnContentUri(uri: String, text: String): Boolean

    /**
     * Get uri of the given file
     */
    suspend fun getUriForFile(file: File, authority: String): Uri

    /**
     * Get offline folder
     */
    suspend fun getOfflineFolder(): File

    /**
     * Get offline file
     */
    suspend fun createNewImageUri(fileName: String): Uri?

    /**
     * Create new video uri
     *
     * @param fileName
     */
    suspend fun createNewVideoUri(fileName: String): Uri?

    /**
     * @return true if the [uriString] represents a file Uri
     */
    suspend fun isFileUri(uriString: String): Boolean

    /**
     * @return true if the [path] represents a file, not a folder
     */
    suspend fun isFilePath(path: String): Boolean

    /**
     * Get the file represented by [uriString]
     *
     * @param uriString must be a file uri (file://...)
     */
    suspend fun getFileFromUriFile(uriString: String): File

    /**
     * @return true if the [uriString] represents a content Uri
     */
    suspend fun isContentUri(uriString: String): Boolean

    /**
     * Is document uri
     *
     * @param uri
     * @return
     */
    suspend fun isDocumentUri(uri: UriPath): Boolean

    /**
     * @return true if the [uriString] represents an external storage content Uri
     */
    suspend fun isExternalStorageContentUri(uriString: String): Boolean

    /**
     * @return the file name of the file identified by [uriString], or null if unknown
     */
    suspend fun getFileNameFromUri(uriString: String): String?

    /**
     * @return the file size of the file identified by [uriString], or null if unknown
     */
    suspend fun getFileSizeFromUri(uriString: String): Long?

    /**
     * Copies the file or folder represented by a content [sourceUri] to the destination File.
     * If [sourceUri] represents a file then [targetFile] must represent a file as well, same for folders.
     * Usually, it is to make a content file returned by a share intent usable by the SDK.
     * @param sourceUri the string representing the file or folder, it must be a "content" uri
     * @param targetFile the destination file or folder where the original file or folder will be copied
     */
    suspend fun copyContentUriToFile(sourceUri: UriPath, targetFile: File)

    /**
     * Creates a new image from [file] to [destination] with [maxPixels] pixels if the image has more than [maxPixels] pixels
     */
    fun downscaleImage(file: File, destination: File, maxPixels: Long)

    /**
     * Delete file by uri
     *
     * @param uri
     * @return
     */
    suspend fun deleteFileByUri(uri: Uri): Boolean

    /**
     * Get files in document folder
     *
     * @param folder
     * @return list of files in the folder
     */
    suspend fun getFilesInDocumentFolder(folder: UriPath): DocumentFolder

    /**
     * Search files in document folder recursive
     *
     * @param folder
     * @param query
     */
    fun searchFilesInDocumentFolderRecursive(
        folder: UriPath,
        query: String,
    ): Flow<DocumentFolder>

    /**
     * Copy files to document uri
     *
     * @param source
     * @param destination
     */
    suspend fun copyFilesToDocumentFolder(
        source: File,
        destination: DocumentFile,
    ): Int

    /**
     * Copy uri to document folder
     *
     * @param source uri
     * @param destination document folder
     */
    suspend fun copyUriToDocumentFolder(
        name: String,
        source: Uri,
        destination: DocumentFile,
    )
}
