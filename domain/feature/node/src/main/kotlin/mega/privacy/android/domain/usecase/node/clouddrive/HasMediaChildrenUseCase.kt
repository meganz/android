package mega.privacy.android.domain.usecase.node.clouddrive

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Whether a folder has at least one image or video among its direct children
 *
 * @property nodeRepository
 */
class HasMediaChildrenUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodeId the parent folder
     * @param excludeSensitives when true, sensitive (hidden) media does not count
     */
    suspend operator fun invoke(nodeId: NodeId, excludeSensitives: Boolean): Boolean =
        nodeRepository.hasMediaChildren(
            nodeId = nodeId,
            sensitivityFilter = SensitivityFilterOption.NonSensitiveOnly.takeIf { excludeSensitives },
        )
}
