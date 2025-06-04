package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.utils.wrapper.LegacyNodeWrapper
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Use case to get node information by handle.
 *
 * @property megaNodeRepository MegaNodeRepository
 * @property getNodeByIdUseCase Use case to get a node by its ID.
 */
class GetLegacyNodeWrapperUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {
    /**
     * Get the node info corresponding to a handle
     *
     * @param handle
     * @return A NodeInfo object corresponding to the given handle, null if cannot be retrieved
     */
    suspend operator fun invoke(handle: Long): LegacyNodeWrapper? {
        val megaNode = megaNodeRepository.getNodeByHandle(handle) ?: return null
        val typedNode = getNodeByIdUseCase(NodeId(handle)) ?: return null
        return LegacyNodeWrapper(
            node = megaNode,
            typedNode = typedNode,
        )
    }
}