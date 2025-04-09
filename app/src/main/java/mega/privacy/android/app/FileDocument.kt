package mega.privacy.android.app

import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * File system document representation
 * @param name
 * @param uriPath
 * @param isFolder
 * @param size
 * @param numFiles
 * @param numFolders
 * @param parent
 * @param canRead
 * @param isHighlighted
 *
 **/
data class FileDocument(
    val name: String,
    val uriPath: UriPath,
    val isFolder: Boolean,
    val size: Long,
    val numFiles: Int,
    val numFolders: Int = 0,
    val parent: FileDocument? = null,
    val canRead: Boolean = false,
    val isHighlighted: Boolean = false,
) {
    constructor(
        documentEntity: DocumentEntity,
        parent: FileDocument?,
        isHighlighted: Boolean = false,
    ) : this(
        name = documentEntity.name,
        uriPath = documentEntity.uri,
        isFolder = documentEntity.isFolder,
        size = documentEntity.size,
        numFiles = documentEntity.numFiles,
        numFolders = documentEntity.numFolders,
        parent = parent,
        canRead = documentEntity.canRead,
        isHighlighted = isHighlighted,
    )

    /**
     * total files and folders children
     */
    val totalChildren = numFiles + numFolders

    /**
     * If file is hidden, checking if name starts with `.`
     */
    val isHidden: Boolean get() = name.startsWith(".")

    /**
     * Gets the uri of this file (example `file:///path/filename.txt`)
     */
    fun getUri() = uriPath.toUri()
}
