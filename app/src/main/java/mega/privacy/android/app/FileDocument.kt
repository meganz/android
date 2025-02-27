package mega.privacy.android.app

import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath

/*
 * File system document representation
 */
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

    val totalChildren = numFiles + numFolders

    val isHidden: Boolean get() = name.startsWith(".")

    fun getUri() = uriPath.toUri()
}
