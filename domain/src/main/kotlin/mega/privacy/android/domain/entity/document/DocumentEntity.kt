package mega.privacy.android.domain.entity.document

import mega.privacy.android.domain.entity.uri.UriPath
import java.io.File

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
    val isFolder: Boolean = false,
    val numFiles: Int = 0,
    val numFolders: Int = 0,
)

/**
 * Creates DocumentEntity from file
 */
fun File.toDocumentEntity() = DocumentEntity(
    name = name,
    size = length(),
    lastModified = lastModified(),
    uri = UriPath(absolutePath),
)