package mega.privacy.android.app.presentation.zipbrowser.mapper

import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.mapper.file.FolderInfoStringMapper
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.icon.pack.R
import javax.inject.Inject

/**
 * The mapper class to convert the ZipTreeNode to ZipInfoUiEntity
 */
class ZipInfoUiEntityMapper @Inject constructor(
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val folderInfoStringMapper: FolderInfoStringMapper,
    private val fileTypeIconMapper: FileTypeIconMapper,
) {

    /**
     * Convert the ZipTreeNode to ZipInfoUiEntity
     */
    operator fun invoke(zipTreeNode: ZipTreeNode) = ZipInfoUiEntity(
        icon = when (zipTreeNode.zipEntryType) {
            ZipEntryType.Folder -> R.drawable.ic_folder_medium_solid
            else -> fileTypeIconMapper(zipTreeNode.getZipFileExtension())
        },
        name = zipTreeNode.name,
        path = zipTreeNode.path,
        info = if (zipTreeNode.zipEntryType == ZipEntryType.Folder) {
            folderInfoStringMapper(zipTreeNode.getFolderNumber(), zipTreeNode.getFileNumber())
        } else {
            fileSizeStringMapper(zipTreeNode.size)
        },
        zipEntryType = zipTreeNode.zipEntryType
    )

    private fun ZipTreeNode.getZipFileExtension() = this.name.substringAfterLast('.', "")

    private fun ZipTreeNode.getFileNumber() = children.filter { child ->
        child.zipEntryType != ZipEntryType.Folder
    }.size

    private fun ZipTreeNode.getFolderNumber() = children.filter { child ->
        child.zipEntryType == ZipEntryType.Folder
    }.size
}