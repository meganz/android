package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to return label list
 */
class GetNodeLabelListUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * invoke
     * @return list of NodeLabel
     */
    operator fun invoke() = nodeRepository.getNodeLabelList()
}