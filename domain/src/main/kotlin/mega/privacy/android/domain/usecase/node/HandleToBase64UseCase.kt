package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to convert a node handle to its Base 64 string.
 */
class HandleToBase64UseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(handle: Long): String =
        nodeRepository.convertHandleToBase64(handle)
}
