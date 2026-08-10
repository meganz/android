package mega.privacy.android.data.filewrapper

import androidx.annotation.Keep
import mega.privacy.android.data.extensions.isFile
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.entity.uri.UriPath
import timber.log.Timber

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
    private val getPathFunction: () -> String?,
    private val deleteFileFunction: () -> Boolean,
    private val deleteFolderIfEmptyFunction: () -> Boolean,
    private val setModificationTimeFunction: (newTime: Long) -> Boolean,
    private val renameFunction: (newName: String) -> FileWrapper?,
    private val renameOverwriteFunction: (parentUriString: String, newName: String, override: Boolean) -> FileWrapper?,
    private val moveDocumentFunction: (sourceParentUriString: String, targetParentUriString: String) -> FileWrapper?,
    private val createNestedPathFunction: (
        children: List<String>,
        createIfMissing: Boolean,
        lastAsFolder: Boolean,
    ) -> String?,
    // Batch metadata for directory scanning — replaces N per-child getPath() + fromUri() calls.
    // Returns null on failure (e.g. SAF query failure or unresolvable parent), which the JNI
    // side propagates to directoryScan as SCAN_INACCESSIBLE. Empty list means "directory really
    // has zero children" (or this wrapper is a non-folder) and is a successful result.
    private val getChildrenWithMetadataFunction: () -> List<ChildMetadata>?,
) {

    /**
     * Get the detached file descriptor for this file or folder.
     * @param write if true, the file descriptor will be opened in write mode, otherwise in read mode
     * @return the file descriptor or null if the operation failed
     */
    @Keep
    fun getFileDescriptor(write: Boolean): Int? = runCatching {
        getDetachedFileDescriptorFunction(write)
    }.onFailure { Timber.e(it) }
        .getOrNull()

    /**
     * Get the children uris of this folder.
     * @return the list of children uris or an empty list if the file is not a folder
     */
    @Keep
    fun getChildrenUris(): List<String> = runCatching { getChildrenUrisFunction() }
        .onFailure { Timber.e(it) }
        .getOrDefault(emptyList())

    /**
     * Check if a child file exists.
     * @param name the name of the file to check
     * @return true if the child file exists, false otherwise
     */
    @Keep
    fun childFileExists(name: String): Boolean = runCatching { childFileExistsFunction(name) }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)

    /**
     * Create a child file or folder.
     * @param name the name of the new file or folder
     * @param asFolder if true, a folder will be created, otherwise a file
     * @return the [FileWrapper] of the newly created file or folder, or null if the operation failed
     */
    @Keep
    fun createChildFile(name: String, asFolder: Boolean): FileWrapper? = runCatching {
        createChildFileFunction(name, asFolder)
    }.onFailure { Timber.e(it) }
        .getOrNull()

    /**
     * Get the path of the file or folder.
     * @return the path or null if the operation failed
     */
    @Keep
    fun getPath(): String? = runCatching { getPathFunction() }
        .onFailure { Timber.e(it) }
        .getOrNull()

    /**
     * Delete the file or folder.
     * @return true if the operation was successful, false otherwise
     */
    @Keep
    fun deleteFile(): Boolean = runCatching { deleteFileFunction() }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)

    /**
     * Delete the folder if it's empty.
     * @return true if the operation was successful, false otherwise
     */
    @Keep
    fun deleteFolderIfEmpty(): Boolean = runCatching { deleteFolderIfEmptyFunction() }
        .onFailure { Timber.e(it) }
        .getOrDefault(false)

    /**
     * Sets the modification time of the file or folder.
     * @param newTime the new modification time in seconds since epoch
     * @return true if the operation was successful, false otherwise
     */
    @Keep
    fun setModificationTime(newTime: Long): Boolean = runCatching {
        setModificationTimeFunction(newTime)
    }.onFailure { Timber.e(it) }
        .getOrDefault(false)

    /**
     * Rename the file or folder.
     * @param newName the new name of the file or folder
     * @return the [FileWrapper] of the renamed file or folder, or null if the operation failed
     */
    @Keep
    fun rename(newName: String): FileWrapper? = runCatching { renameFunction(newName) }
        .onFailure { Timber.e(it) }
        .getOrNull()

    /**
     * Rename the file or folder.
     * @param parentUriString The uri of the parent file.
     * @param newName the new name of the file or folder
     * @param override True if it is required to override an existing file, false otherwise.
     * @return the [FileWrapper] of the renamed file or folder, or null if the operation failed
     */
    @Keep
    fun renameOverwrite(parentUriString: String, newName: String, override: Boolean): FileWrapper? =
        runCatching { renameOverwriteFunction(parentUriString, newName, override) }
            .onFailure { Timber.e(it) }
            .getOrNull()

    /**
     * Move the file or folder from [sourceParentUriString] to [targetParentUriString] within the
     * same SAF tree using DocumentsContract.moveDocument (API 24+). Avoids the byte-by-byte copy
     * fallback used by the SDK's cross-parent rename path. The document keeps its original name;
     * if a different target name is required, follow this with a same-parent rename.
     * @return the [FileWrapper] of the moved file or folder, or null if the provider does not
     *   support FLAG_SUPPORTS_MOVE or the operation otherwise failed.
     */
    @Keep
    fun moveDocument(sourceParentUriString: String, targetParentUriString: String): FileWrapper? =
        runCatching { moveDocumentFunction(sourceParentUriString, targetParentUriString) }
            .onFailure { Timber.w(it) }
            .getOrNull()


    /**
     * Get a child file by name.
     * @param name the name of the child file
     * @return the URI of the child file or null if the operation failed
     */
    @Keep
    fun getChildByName(name: String): String? = runCatching { getChildByNameFunction(name) }
        .onFailure { Timber.e(it) }
        .getOrNull()

    @Keep
    fun createNestedPath(
        children: List<String>,
        createIfMissing: Boolean,
        lastAsFolder: Boolean,
    ): String? = runCatching { createNestedPathFunction(children, createIfMissing, lastAsFolder) }
        .onFailure { Timber.e(it) }
        .getOrNull()

    /**
     * Returns a batch list of [ChildMetadata] for all direct children of this folder,
     * populated via a single [android.content.ContentResolver.query] call.
     *
     * Called from C++ [directoryScan] to replace the previous N+1 IPC pattern:
     *   1 [getChildren()] + N [getPath()] + N [getFileDescriptor()] calls.
     *
     * Returns null on failure (error is logged) so the C++ side can fall back to
     * [SCAN_INACCESSIBLE].
     */
    @Keep
    fun getChildrenWithMetadata(): List<ChildMetadata>? = runCatching {
        getChildrenWithMetadataFunction()
    }.onFailure { Timber.e(it) }.getOrNull()


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
        fun getFromUri(uriPath: String): FileWrapper? = factory(UriPath(uriPath))

        /**
         * Checks whether the SAF document at [uriPath] currently exists by issuing a
         * fresh ContentResolver query via [FileWrapperFactory] → [DocumentFileWrapper.fromUri]
         * with the existence probe enabled.
         *
         * Used by the C++ cache-validation paths to detect stale wrappers (e.g. the
         * underlying folder was deleted externally between sync sessions). The cheap
         * Java-reference check on the C++ side cannot detect that case.
         *
         * @return true if the document is currently resolvable on SAF; false otherwise.
         */
        @JvmStatic
        @Keep
        fun existsOnSaf(uriPath: String): Boolean = runCatching {
            factory(UriPath(uriPath)) != null
        }.onFailure { Timber.w(it, "existsOnSaf failed for $uriPath") }
            .getOrDefault(false)

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
