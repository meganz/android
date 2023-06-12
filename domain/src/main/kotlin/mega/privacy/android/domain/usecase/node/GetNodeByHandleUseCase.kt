package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get node by handles use case
 *
 */
class GetNodeByHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param handle
     */
    suspend operator fun invoke(handle: Long) =
        nodeRepository.getNodeByHandle(handle)
}