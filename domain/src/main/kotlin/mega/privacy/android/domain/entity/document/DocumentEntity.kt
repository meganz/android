package mega.privacy.android.domain.entity.document

import mega.privacy.android.domain.entity.uri.UriPath
import java.io.File

/**
 * Document entity
 *
 * @property name the name of this document, it can be the original name or modified name if the user changed it
 * @property size
 * @property lastModified
 * @property uri
 * @property isFolder
 * @property numFiles
 * @property numFolders
 * @property originalName the original name of this document
 */
data class DocumentEntity(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val uri: UriPath,
    val isFolder: Boolean = false,
    val numFiles: Int = 0,
    val numFolders: Int = 0,
    val originalName: String = name,
){
    /**
     * get uri value for java access, it should be removed once ImportFilesAdapter.java is removed
     */
    fun getUriString() = uri.value
}

/**
 * Creates DocumentEntity from file
 */
fun File.toDocumentEntity(customName: String? = null) = DocumentEntity(
    originalName = name,
    name = customName ?: name,
    size = length(),
    lastModified = lastModified(),
    uri = UriPath(absolutePath),
)