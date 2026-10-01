package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.UnTypedNode
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
     * @param handle [Long] node handle
     * @param attemptFromFolderApi [Boolean] true if needs to be checked from folder API
     * and get authorized, for example when node is opened from link
     *
     * @return [UnTypedNode]
     */
    suspend operator fun invoke(handle: Long, attemptFromFolderApi: Boolean = false) =
        nodeRepository.getNodeByHandle(handle, attemptFromFolderApi)
}