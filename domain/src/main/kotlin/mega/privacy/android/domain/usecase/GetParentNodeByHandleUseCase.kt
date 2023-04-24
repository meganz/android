package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting parent node by handle
 */
class GetParentNodeByHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get parent node by handle
     *
     * @param parentHandle node handle
     * @return [UnTypedNode]?
     */
    suspend operator fun invoke(parentHandle: Long) =
        mediaPlayerRepository.getParentNodeByHandle(parentHandle)
}