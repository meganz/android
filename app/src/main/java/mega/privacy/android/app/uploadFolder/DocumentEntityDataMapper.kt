package mega.privacy.android.app.uploadFolder

import android.net.Uri
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.domain.entity.document.DocumentEntity
import javax.inject.Inject

/**
 * Document entity data mapper
 *
 */
class DocumentEntityDataMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param parent
     * @param entity
     */
    operator fun invoke(parent: FolderContent.Data, entity: DocumentEntity): FolderContent.Data =
        FolderContent.Data(
            parent = parent,
            name = entity.name,
            isFolder = entity.isFolder,
            lastModified = entity.lastModified,
            size = entity.size,
            numberOfFiles = entity.numFiles,
            numberOfFolders = entity.numFolders,
            uri = Uri.parse(entity.uri.value),
        )
}