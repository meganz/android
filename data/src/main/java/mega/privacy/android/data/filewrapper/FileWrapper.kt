package mega.privacy.android.data.filewrapper

import androidx.annotation.Keep
import mega.privacy.android.data.extensions.isFile
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Class used by SDK via JNI to get access to the native file descriptor (fd) and metadata of a file or folder. It's used to access files and folders from a content uri or path in a platform agnostic way.
 * It should be used by native code only.
 */
@Keep
class FileWrapper(
    val uri: String,
    val name: String,
    val isFolder: Boolean,
    private val getChildrenUrisFunction: () -> List<String>,
    private val getDetachedFileDescriptorFunction: (write: Boolean) -> Int?,
    private val childFileExistsFunction: (name: String) -> Boolean,
    private val getChildByNameFunction: (name: String) -> String?,
    private val createChildFileFunction: (name: String, asFolder: Boolean) -> FileWrapper?,
    private val getParentUriFunction: () -> FileWrapper?,
    private val getPathFunction: () -> String?,
    private val deleteFileFunction: () -> Boolean,
    private val deleteFolderIfEmptyFunction: () -> Boolean,
    private val setModificationTimeFunction: (newTime: Long) -> Boolean,
    private val renameFunction: (newName: String) -> FileWrapper?,
) {

    /**
     * Get the detached file descriptor for this file or folder.
     * @param write if true, the file descriptor will be opened in write mode, otherwise in read mode
     * @return the file descriptor or null if the operation failed
     */
    @Keep
    fun getFileDescriptor(write: Boolean) =
        getDetachedFileDescriptorFunction(write)

    /**
     * Get the children uris of this folder.
     * @return the list of children uris or an empty list if the file is not a folder
     */
    @Keep
    fun getChildrenUris(): List<String> = getChildrenUrisFunction()

    /**
     * Check if a child file exists.
     * @param name the name of the file to check
     * @return true if the child file exists, false otherwise
     */
    @Keep
    fun childFileExists(name: String) = childFileExistsFunction(name)

    /**
     * Create a child file or folder.
     * @param name the name of the new file or folder
     * @param asFolder if true, a folder will be created, otherwise a file
     * @return the [FileWrapper] of the newly created file or folder, or null if the operation failed
     */
    @Keep
    fun createChildFile(name: String, asFolder: Boolean) = createChildFileFunction(name, asFolder)

    /**
     * Get the parent file or folder.
     * @return the [FileWrapper] of the parent folder or null if the operation failed
     */
    @Keep
    fun getParentFile(): FileWrapper? = getParentUriFunction()

    /**
     * Get the path of the file or folder.
     * @return the path or null if the operation failed
     */
    @Keep
    fun getPath(): String? = getPathFunction()

    /**
     * Delete the file or folder.
     * @return true if the operation was successful, false otherwise
     */
    @Keep
    fun deleteFile(): Boolean = deleteFileFunction()

    /**
     * Delete the folder if it's empty.
     * @return true if the operation was successful, false otherwise
     */
    @Keep
    fun deleteFolderIfEmpty(): Boolean = deleteFolderIfEmptyFunction()

    /**
     * Sets the modification time of the file or folder.
     * @param newTime the new modification time in seconds since epoch
     * @return true if the operation was successful, false otherwise
     */
    @Keep
    fun setModificationTime(newTime: Long): Boolean = setModificationTimeFunction(newTime)

    /**
     * Rename the file or folder.
     * @param newName the new name of the file or folder
     * @return the [FileWrapper] of the renamed file or folder, or null if the operation failed
     */
    @Keep
    fun rename(newName: String): FileWrapper? = renameFunction(newName)


    /**
     * Get a child file by name.
     * @param name the name of the child file
     * @return the URI of the child file or null if the operation failed
     */
    @Keep
    fun getChildByName(name: String): String? =
        getChildByNameFunction(name)


    companion object {

        /**
         * As this is used by native code, we can't use dependency injection directly, we need static methods
         */
        fun initializeFactory(
            fileGateway: FileGateway,
        ) {
            factory = FileWrapperFactory(fileGateway)
        }

        private lateinit var factory: FileWrapperFactory

        /**
         * Returns [FileWrapper] from [uriPath] string.
         * @param uriPath Usually the content uri of the file, but it can be a path also
         */
        @JvmStatic
        @Keep
        fun getFromUri(uriPath: String) = factory(UriPath(uriPath))

        /**
         * Static method to check if a string represents a file as opposed to an Uri.
         * This method doesn't check if the path is valid or points to an existing File or not.
         * @return true if this string represents a file path, false otherwise
         */
        @Keep
        @JvmStatic
        fun isPath(path: String) = path.startsWith("file").not() && UriPath(path).isFile()
    }
}
