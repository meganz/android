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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
     * Get disk space at the given path.
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
     * Checks whether the External Storage Directory exists
     *
     * @return true if it exists, and false if otherwise
     */
    suspend fun doesExternalStorageDirectoryExists(): Boolean

    /**
     * Get External Storage Directory Path
     *
     */
    suspend fun getExternalStorageDirectoryPath(): String


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
     * Takes an Uri and creates an external storage path from it if possible.
     *
     * E.g. the following content Uri:
     * "content://com.android.externalstorage.documents/tree/primary%3ASync%2FsomeFolder"
     * will be converted to * "/storage/emulated/0/Sync/someFolder".
     * And the following file Uri:
     * "file:///storage/emulated/0/xyzrutazyx/.megaignore" will be converted to
     * "/storage/emulated/0/xyzrutazyx/.megaignore".
     *
     * Note that if a path is passed as uri, it will be returned as is.
     *
     * @param uriString The Uri to be converted
     */
    suspend fun getExternalPathByUri(uriString: String): String?

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
     * @param uriPath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getVideoGPSCoordinates(uriPath: UriPath): Pair<Double, Double>?

    /**
     * Get GPS coordinates from photo file
     *
     * @param uriPath
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
     * @return true if the [uriString] represents a file Uri (`file:///...`)
     */
    suspend fun isFileUri(uriString: String): Boolean

    /**
     * @return true if the [path] represents a file, not a folder
     */
    suspend fun isFilePath(path: String): Boolean

    /**
     * @return true if the [path] represents a folder
     */
    suspend fun isFolderPath(path: String): Boolean

    /**
     * @return true if the [uri] represents a folder content uri (`content://com.android.externalstorage.documents/tree/...`)
     */
    suspend fun isFolderContentUri(uri: String): Boolean

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
     * Creates a new image from [original] to [destination] with [maxPixels] pixels if the image has more than [maxPixels] pixels
     */
    suspend fun downscaleImage(original: UriPath, destination: File, maxPixels: Long)

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
     * Get file type info for a given UriPath and file name
     */
    suspend fun getFileTypeInfo(uriPath: UriPath, fileName: String): FileTypeInfo

    /**
     * Delete file by uri by content resolver for MediaStore
     *
     * @param uri
     * @return true if the file is deleted successfully
     */
    suspend fun deleteFileByUri(uri: String): Boolean


    /**
     * Delete a document file by its content uri.
     *
     * @param uriPath
     * @return true if the file is deleted successfully
     */
    suspend fun deleteDocumentFileByContentUri(uriPath: UriPath): Boolean

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

    /**
     * Checks if an uri path exists
     *
     * @param uriPath the [UriPath] to check
     * @return true if the UriPath exists, false otherwise
     */
    suspend fun doesUriPathExist(uriPath: UriPath): Boolean

    /**
     * Remove persistent permission for the given uri
     *
     * @param uriPath the [UriPath] to check
     */
    suspend fun removePersistentPermission(uriPath: UriPath)

    /**
     * Checks if the uri has persisted permission
     *
     * @param uri
     * @param writePermission If true write permission will be checked as well, only read permission otherwise
     */
    suspend fun hasPersistedPermission(uriPath: UriPath, writePermission: Boolean): Boolean

    /**
     * Takes persisted permission of the given Uri, this may throw security exception if the permission has not been granted or it's outdated
     */
    suspend fun takePersistablePermission(uriPath: UriPath, writePermission: Boolean)

    /**
     * Get [DocumentEntity] given uri string.
     *
     * Note that, for downloads, we have this uri but it is not the real DocumentFile uri,
     * so we need to find the real DocumentFile uri by using the given uriString, getting
     * the tree uri DocumentFile and then, finding the child by the file name.
     *
     * @param uriString The string Uri of the file, usually a content uri
     * @return The [DocumentEntity] of the file.
     */
    suspend fun getDocumentFileIfContentUri(uriString: String): DocumentEntity?

    /**
     * Get [DocumentEntity] given uri string and file name.
     *
     * Note that, for downloads, we have this uri but it is not the real DocumentFile uri,
     * so we need to find the real DocumentFile uri by using the given uriString, getting
     * the tree uri DocumentFile and then, finding the child by the file name.
     *
     * @param uriString The string Uri of the file, usually a content uri
     * @param fileName The name of the file.
     * @return The [DocumentEntity] of the file.
     */
    suspend fun getDocumentFileIfContentUri(uriString: String, fileName: String): DocumentEntity?

    /**
     * Get the last modified time of a file [UriPath]
     *
     * @param uriPath [UriPath] to be obtained from
     * @return the last modified time in milliseconds since epoch, or null if the time cannot be get
     */
    @OptIn(ExperimentalTime::class)
    suspend fun getLastModifiedTime(uriPath: UriPath): Instant?

    /**
     * Renames a document with incremented counter with the same name in the same folder.
     *
     * @param uriPaths list of [UriPath] to be obtained from
     *
     */
    suspend fun renameDocumentWithTheSameName(uriPaths: List<UriPath>)
}
