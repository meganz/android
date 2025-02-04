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
    private val createChildFileFunction: (name: String, asFolder: Boolean) -> FileWrapper?,
    private val getParentUriFunction: () -> FileWrapper?,
    private val getPathFunction: () -> String?,
    private val deleteFileFunction: () -> Boolean,
    private val deleteFolderIfEmptyFunction: () -> Boolean,
    private val setModificationTimeFunction: (newTime: Long) -> Boolean,
    private val renameFunction: (newName: String) -> FileWrapper?,
) {

    @Keep
    fun getFileDescriptor(write: Boolean) =
        getDetachedFileDescriptorFunction(write)

    @Keep
    fun getChildrenUris(): List<String> = getChildrenUrisFunction()

    @Keep
    fun childFileExists(name: String) = childFileExistsFunction(name)

    @Keep
    fun createChildFile(name: String, asFolder: Boolean) = createChildFileFunction(name, asFolder)

    @Keep
    fun getParentFile(): FileWrapper? = getParentUriFunction()

    @Keep
    fun getPath(): String? = getPathFunction()

    @Keep
    fun deleteFile(): Boolean = deleteFileFunction()

    @Keep
    fun deleteFolderIfEmpty(): Boolean = deleteFolderIfEmptyFunction()

    @Keep
    fun setModificationTime(newTime: Long): Boolean = setModificationTimeFunction(newTime)

    @Keep
    fun rename(newName: String): FileWrapper? = renameFunction(newName)


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