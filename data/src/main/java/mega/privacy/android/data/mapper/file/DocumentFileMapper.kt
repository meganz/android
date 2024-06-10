package mega.privacy.android.data.mapper.file

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
        numFolders = numFolders
    )
}