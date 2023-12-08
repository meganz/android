package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Check if Camera Uploads folders are in rubbish bin or deleted
 * based on the [NodeUpdate] received
 */
class AreCameraUploadsFoldersInRubbishBinUseCase @Inject constructor(
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
) {

    /**
     * Invoke
     * @param primaryHandle Primary Folder Handle
     * @param secondaryHandle Secondary Folder Handle
     * @param nodeUpdate a node update received
     * @return true if either the camera uploads primary folder or secondary folder
     *         have been moved to rubbish bin or deleted
     */
    suspend operator fun invoke(
        primaryHandle: Long,
        secondaryHandle: Long,
        nodeUpdate: NodeUpdate,
    ): Boolean {
        nodeUpdate.changes.entries.firstOrNull { set ->
            (set.key.id.longValue == primaryHandle || set.key.id.longValue == secondaryHandle)
                    && set.value.contains(NodeChanges.Attributes)
        } ?: return false

        val isPrimaryUploadFolderInRubbishBinOrDeleted =
            isNodeInRubbishOrDeletedUseCase(primaryHandle)
        val isSecondaryUploadFolderInRubbishBinOrDeleted =
            isSecondaryFolderEnabled() && isNodeInRubbishOrDeletedUseCase(secondaryHandle)

        return (isPrimaryUploadFolderInRubbishBinOrDeleted || isSecondaryUploadFolderInRubbishBinOrDeleted)
    }
}
