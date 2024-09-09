package mega.privacy.android.domain.usecase.photos.mediadiscovery

import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import javax.inject.Inject

/**
 * Use case to check if media discovery mode should be entered
 *
 * @property getRootNodeUseCase                       GetRootNodeUseCase to get the root node
 * @property getCloudSortOrder                        GetCloudSortOrder to get the cloud sort order
 * @property nodeRepository                           NodeRepository to manage nodes
 */
class ShouldEnterMediaDiscoveryModeUseCase @Inject constructor(
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val nodeRepository: NodeRepository,
) {

    suspend operator fun invoke(
        parentHandle: Long,
    ): Boolean {
        val nodeId =
            (if (parentHandle != nodeRepository.getInvalidHandle()) NodeId(parentHandle) else getRootNodeUseCase()?.id)
                ?: return false
        val childNodesFileTypes =
            nodeRepository.getNodeChildrenFileTypes(nodeId = nodeId, order = getCloudSortOrder())
        return childNodesFileTypes.none { fileType ->
            fileType is UnMappedFileTypeInfo ||
                    fileType is SvgFileTypeInfo ||
                    fileType !is ImageFileTypeInfo
                    && fileType !is VideoFileTypeInfo
        }
    }
}