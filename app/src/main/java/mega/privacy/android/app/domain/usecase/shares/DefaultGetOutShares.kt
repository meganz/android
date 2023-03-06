package mega.privacy.android.app.domain.usecase.shares

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Default Implementation of GetOutShares
 */
class DefaultGetOutShares @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
) : GetOutShares {
    override suspend fun invoke(nodeId: NodeId) =
        megaNodeRepository.getOutShares(nodeId)
}