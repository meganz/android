package mega.privacy.android.domain.entity.document

import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Document entity
 *
 * @property name
 * @property size
 * @property lastModified
 * @property uri
 * @property isFolder
 * @property numFiles
 * @property numFolders
 */
data class DocumentEntity(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val uri: UriPath,
    val isFolder: Boolean,
    val numFiles: Int,
    val numFolders: Int,
)