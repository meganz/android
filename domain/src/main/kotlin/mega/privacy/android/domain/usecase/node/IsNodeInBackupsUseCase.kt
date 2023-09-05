package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Use Case that checks whether the given Node from a Node Handle is in Backups or not
 */
class IsNodeInBackupsUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invocation function
     *
     * @param handle The Node Handle
     * @return true if Node from the Node Handle provided is in Backups, and false if otherwise
     */
    suspend operator fun invoke(handle: Long) = nodeRepository.isNodeInBackups(handle)
}