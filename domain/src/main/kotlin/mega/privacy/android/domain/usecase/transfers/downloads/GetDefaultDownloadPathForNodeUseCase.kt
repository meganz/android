package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.GetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.node.GetNestedParentFoldersUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.joinAsPath
import java.io.File
import javax.inject.Inject

/**
 * Get the default path where node should be downloaded
 */
class GetDefaultDownloadPathForNodeUseCase @Inject constructor(
    private val getStorageDownloadLocationUseCase: GetStorageDownloadLocationUseCase,
    private val getNestedParentFoldersUseCase: GetNestedParentFoldersUseCase,
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase,
) {
    /**
     * * Get the default path where nodes should be downloaded given its parent folder
     * @param node the parent folder of the nodes that will be downloaded
     * @return a string
     */
    suspend operator fun invoke(node: Node): String? {
        val location = getStorageDownloadLocationUseCase() ?: return null
        return if (isNodeInCloudDriveUseCase(node.id.longValue)) {
            location.removeSuffix(File.separator) + getNestedParentFoldersUseCase(node)
                .drop(1) //we don't want to add "Cloud Drive" root to the path
                .joinAsPath()
        } else {
            location
        }
    }
}