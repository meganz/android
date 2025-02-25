package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.uri.UriPath
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
     * Get local file
     *
     * @param fileNode
     * @return local file if it exists
     */
    suspend fun getLocalFile(fileNode: FileNode): File?

    /**
     * Get file by path if it exists
     */
    suspend fun getFileByPath(path: String): File?

    /**
     * create temp file in file system
     * @param rootPath root path
     * @param localPath
     * @param destinationPath
     */
    @Throws(IOException::class)
    suspend fun createTempFile(
        rootPath: String,
        localPath: String,
        destinationPath: String,
    ): String

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
     * Creates a temporary Camera Uploads root directory
     */
    suspend fun createCameraUploadsTemporaryRootDirectory(): File?

    /**
     * Recursively deletes the temporary Camera Uploads root directory
     *
     * @return true if the delete operation is successful, and false if otherwise
     */
    suspend fun deleteCameraUploadsTemporaryRootDirectory(): Boolean

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
     * Returns an absolute path based on document content Uri
     */
    suspend fun getAbsolutePathByContentUri(uri: String): String?

    /**
     * Tries to determine the content type of an object,
     * based on the specified "file" component of a URL.
     *
     * @param localPath Local path of the file
     * @return The content type of an object if any.
     */
    suspend fun getGuessContentTypeFromName(localPath: String): String?

    /**
     * Return the MIME type of the given content Uri.
     *
     * @param uriPath UriPath of the file
     * @return The content type of the Uri if any.
     */
    suspend fun getContentTypeFromContentUri(uriPath: UriPath): String?

    /**
     * Get GPS coordinates from video file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getVideoGPSCoordinates(uriPath: UriPath): Pair<Double, Double>?

    /**
     * Get GPS coordinates from photo file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getPhotoGPSCoordinates(uriPath: UriPath): Pair<Double, Double>?

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
    suspend fun getUriForFile(file: File, authority: String): String

    /**
     * Delete folder is its files
     */
    suspend fun deleteFolderAndItsFiles(path: String)

    /**
     * Get offline folder
     */
    suspend fun getOfflineFolder(): File

    /**
     * Get size of file or dir in bytes
     */
    suspend fun getTotalSize(file: File?): Long

    /**
     * Check if file is exists
     */
    suspend fun checkFileExistsByUriPath(uriPath: String?): String?

    /**
     * @return true if the [localPath] points to a SD card
     */
    suspend fun isSDCardPathOrUri(localPath: String): Boolean

    /**
     * @return true if the [localPath] points to a SD card cache
     */
    suspend fun isSDCardCachePath(localPath: String): Boolean

    /**
     * Moves a [file] to a [destinationUri] on the sd. It first copies the file to the [destinationUri] and then deletes the original one
     *
     * @param file the file to be moved
     * @param destinationUri the target uri where the file will be moved (excluding the name of the file itself)
     */
    suspend fun moveFileToSd(file: File, destinationUri: String, subFolders: List<String>): Boolean

    /**
     * Moves a [directory] to a [destinationUri] on the sd. It first copies the file to the [destinationUri] and then deletes the original one
     *
     * @param directory the directory to be moved
     * @param destinationUri the target uri where the file will be moved (excluding the name of the file itself)
     */
    suspend fun moveDirectoryToSd(directory: File, destinationUri: String): Boolean

    /**
     * Create new image uri
     *
     * @param fileName file name
     * @return uri string
     */
    suspend fun createNewImageUri(fileName: String): String?

    /**
     * Create new video uri
     *
     * @param fileName file name
     * @return uri string
     */
    suspend fun createNewVideoUri(fileName: String): String?

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
    suspend fun getFileFromFileUri(uriString: String): File

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
     *  @return the file size of the file identified by [uriString], or null if unknown
     */
    suspend fun getFileSizeFromUri(uriString: String): Long?

    /**
     * Copies the file or folder represented by a content [sourceUri] to the destination File.
     * If [sourceUri] represents a file then [targetFile] must represent a file as well, same for folders.
     * @param sourceUri the string representing the file, it must be a "content" uri
     * @param targetFile the destination file where the original file will be copied
     */
    suspend fun copyContentUriToFile(sourceUri: UriPath, targetFile: File)

    /**
     * @return the files in same folder
     */
    suspend fun getFileSiblingByUri(uriString: String): List<File>

    /**
     * Creates a new image from [file] to [destination] with [maxPixels] pixels if the image has more than [maxPixels] pixels
     */
    suspend fun downscaleImage(file: File, destination: File, maxPixels: Long)

    /**
     * Deletes a voice clip file.
     *
     * @return True if file has been removed, false otherwise
     */
    suspend fun deleteVoiceClip(name: String): Boolean

    /**
     * Get file type info for a given file
     */
    suspend fun getFileTypeInfo(file: File): FileTypeInfo

    /**
     * Delete file by uri
     *
     * @param uri
     * @return true if the file is deleted successfully
     */
    suspend fun deleteFileByUri(uri: String): Boolean

    /**
     * Get files in document folder
     *
     * @param uri file uri of the document folder
     * @return
     */
    suspend fun getFilesInDocumentFolder(uri: UriPath): DocumentFolder

    /**
     * Search files in document folder recursive
     *
     * @param folder
     * @param query
     * @return
     */
    fun searchFilesInDocumentFolderRecursive(
        folder: UriPath,
        query: String,
    ): Flow<DocumentFolder>

    /**
     * Move files to document uri
     *
     * @param source
     * @param destinationUri
     */
    suspend fun copyFilesToDocumentUri(
        source: File,
        destinationUri: UriPath,
    ): Int

    /**
     * Copy files
     *
     * @param source file or folder to copy
     * @param destination destination folder
     */
    suspend fun copyFiles(
        source: File,
        destination: File,
    ): Int

    /**
     * Get file type info for a given name
     *
     * @param name file name
     * @param duration duration of the file
     * @return [FileTypeInfo] object
     */
    fun getFileTypeInfoByName(name: String, duration: Int = 0): FileTypeInfo

    /**
     * Copy uri
     *
     * @param source
     * @param destination
     */
    suspend fun copyUri(name: String, source: UriPath, destination: File)

    /**
     * Copy uri
     *
     * @param source
     * @param destination
     */
    suspend fun copyUri(name: String, source: UriPath, destination: UriPath)

    /**
     * Check if the path is malformed from an external app
     *
     * @param action
     * @param path
     * @return true if the path is malformed
     */
    fun isMalformedPathFromExternalApp(action: String?, path: String): Boolean

    /**
     * Check if the path is insecure
     *
     * @param path
     * @return true if the path is insecure
     */
    fun isPathInsecure(path: String): Boolean

    /**
     * Get document file name
     *
     * @param uri
     * @return file name
     */
    suspend fun getDocumentFileName(uri: UriPath): String

    /**
     * Get a list of [DocumentEntity]s from a list of [UriPath]s, non-existing documents are filtered out
     */
    suspend fun getDocumentEntities(uris: List<UriPath>): List<DocumentEntity>

    /**
     * Get a [DocumentEntity] from an [UriPath], or null if it doesn't exist
     */
    suspend fun getDocumentEntity(uri: UriPath): DocumentEntity?

    /**
     * Get file from uri
     *
     * @param uri
     * @return
     */
    suspend fun getFileFromUri(uri: UriPath): File?

    /**
     * Given the original File's Uri path, this creates a new File with the specified Filename and
     * deletes the original File
     *
     * @param originalUriPath The original File's Uri path
     * @param newFilename The File name to use for the new File
     *
     * @return The renamed File
     */
    suspend fun renameFileAndDeleteOriginal(
        originalUriPath: UriPath,
        newFilename: String,
    ): File

    /**
     * Gets the length of the file given the content URI.
     */
    suspend fun getFileLengthFromSdCardContentUri(fileContentUri: String): Long

    /**
     * Deletes the file given the content URI.
     */
    suspend fun deleteFileFromSdCardContentUri(fileContentUri: String): Boolean

    /**
     * Checks if an uri can be read
     *
     * @param stringUri the uri to check
     * @return true if the uri can be read, false otherwise
     */
    suspend fun canReadUri(stringUri: String): Boolean

    /**
     * Returns Offline Files Root Folder
     */
    suspend fun getOfflineFilesRootFolder(): File

    /**
     * Returns device model or SD Card based on file location
     */
    suspend fun getFileStorageTypeName(path: String?): FileStorageType
}
