package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

class GetNodeLabelUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {
    operator fun invoke(labelId: Int) = nodeRepository.getNodeLabel(labelId)
}