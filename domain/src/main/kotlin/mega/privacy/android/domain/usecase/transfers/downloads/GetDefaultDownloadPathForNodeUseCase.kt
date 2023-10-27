package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.GetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.node.GetNestedParentFoldersUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.joinAsPath
import java.io.File
import javax.inject.Inject

/**
 * Get the default path where nodes should be downloaded given its parent folder
 */
class GetDefaultDownloadPathForNodeUseCase @Inject constructor(
    private val getStorageDownloadLocationUseCase: GetStorageDownloadLocationUseCase,
    private val getNestedParentFoldersUseCase: GetNestedParentFoldersUseCase,
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
) {
    /**
     * * Get the default path where nodes should be downloaded given its parent folder
     * @param parentFolder the parent folder of the nodes that will be downloaded
     * @return a string
     */
    suspend operator fun invoke(parentFolder: FolderNode): String? {
        val location = getStorageDownloadLocationUseCase() ?: return null
        return if (isNodeInCloudDriveUseCase(parentFolder.id.longValue)) {
            location.removeSuffix(File.separator) + getNestedParentFoldersUseCase(parentFolder)
                .plus(parentFolder)//the parent itself needs to be added too
                .drop(1) //we don't want to add "Cloud Drive" root to the path
                .joinAsPath()
        } else {
            location
        }
    }
}