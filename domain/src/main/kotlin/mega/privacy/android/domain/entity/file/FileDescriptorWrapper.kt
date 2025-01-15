package mega.privacy.android.domain.entity.file

import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Class to get the necessary data of a file or folder for native upload
 * @property uriPath The original uriPath
 * @property name File or folder's name
 * @property isFolder If true it represents a folder, it represents a file otherwise
 */
data class FileDescriptorWrapper(
    val uriPath: UriPath,
    val name: String,
    val isFolder: Boolean,
    private val getChildrenUrisFunction: () -> List<UriPath>,
    private val getDetachedFileDescriptorFunction: (write: Boolean) -> Int?,
) {
    private var detachedFileDescriptor: Int? = null

    /**
     * Returns a native file descriptor integer (fd) to be used by native code to access the file. This fd is detached, so the native code is responsible to close it when it's not needed anymore.
     * To avoid multiple opened file descriptors for the same instance, the result is cached, so it's save to call this function multiple times.
     */
    fun getDetachedFileDescriptor(write: Boolean) =
        (detachedFileDescriptor ?: getDetachedFileDescriptorFunction(write)?.also {
            detachedFileDescriptor = it
        }) ?: -1

    /**
     * Get the uris of the children of this entity. If it's a file or an empty folder, an empty list is returned
     */
    fun getChildrenUris() = getChildrenUrisFunction()
}