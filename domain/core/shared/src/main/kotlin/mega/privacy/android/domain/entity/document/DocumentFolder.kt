package mega.privacy.android.domain.entity.document

import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Document folder
 *
 * @property files List of [UriPath] that represents the files in the folder
 */
data class DocumentFolder(
    val files: List<DocumentEntity>,
)