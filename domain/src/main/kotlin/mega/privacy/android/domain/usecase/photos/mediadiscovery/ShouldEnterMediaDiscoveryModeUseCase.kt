package mega.privacy.android.domain.usecase.photos.mediadiscovery

import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
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
        isCheckSensitive: Boolean = false,
    ): Boolean {
        val nodeId = if (parentHandle != nodeRepository.getInvalidHandle()) {
            NodeId(parentHandle)
        } else {
            getRootNodeUseCase()?.id
        } ?: return false

        val nodes = nodeRepository.getTypedNodesById(
            nodeId = nodeId,
            order = getCloudSortOrder()
        )

        if (nodes.isEmpty()) return false

        val isAllImageOrVideo = nodes.all { node ->
            node is FileNode && node.type !is SvgFileTypeInfo &&
                    (node.type is ImageFileTypeInfo || node.type is VideoFileTypeInfo)
        }

        return if (isAllImageOrVideo && isCheckSensitive) {
            !nodes.filterIsInstance<FileNode>().all { it.isMarkedSensitive }
        } else {
            isAllImageOrVideo
        }
    }
}