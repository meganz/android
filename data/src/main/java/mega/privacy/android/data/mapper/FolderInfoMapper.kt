package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.FolderInfo
import nz.mega.sdk.MegaFolderInfo
import javax.inject.Inject

/**
 * Map [MegaFolderInfo] to [FolderInfo]
 */
internal class FolderInfoMapper @Inject constructor() {
    operator fun invoke(megaFolderInfo: MegaFolderInfo, folderName: String): FolderInfo =
        with(megaFolderInfo) {
            FolderInfo(
                currentSize = currentSize,
                numVersions = numVersions,
                numFiles = numFiles,
                numFolders = numFolders,
                versionsSize = versionsSize,
                folderName = folderName,
            )
        }
}