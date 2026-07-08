package mega.privacy.android.data.mapper.file

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import javax.inject.Inject

/**
 * Document file mapper
 *
 */
internal class DocumentFileMapper @Inject constructor() {
    operator fun invoke(file: DocumentFile, numFiles: Int, numFolders: Int) = DocumentEntity(
        name = file.name.orEmpty(),
        size = file.length(),
        lastModified = file.lastModified(),
        uri = UriPath(file.uri.toString()),
        isFolder = file.isDirectory,
        numFiles = numFiles,
        numFolders = numFolders,
        canRead = file.canRead(),
    )

    /**
     * Build a [DocumentEntity] directly from raw metadata fetched via
     * `ContentResolver.query()`. Avoids the per-property IPC cost incurred by the
     * [DocumentFile] overload above.
     *
     * `canRead` defaults to true: callers reach this overload only after iterating
     * the children of a parent the app already has read permission on.
     */
    operator fun invoke(
        uri: Uri,
        name: String,
        size: Long,
        lastModified: Long,
        isFolder: Boolean,
        numFiles: Int,
        numFolders: Int,
        canRead: Boolean = true,
    ) = DocumentEntity(
        name = name,
        size = size,
        lastModified = lastModified,
        uri = UriPath(uri.toString()),
        isFolder = isFolder,
        numFiles = numFiles,
        numFolders = numFolders,
        canRead = canRead,
    )
}