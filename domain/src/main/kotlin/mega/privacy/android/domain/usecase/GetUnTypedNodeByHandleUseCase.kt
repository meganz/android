package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for getting UnTypedNode by handle
 */
class GetUnTypedNodeByHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get UnTypedNode by handle
     *
     * @param handle node handle
     * @return UnTypedNode
     */
    suspend operator fun invoke(handle: Long) = mediaPlayerRepository.getUnTypedNodeByHandle(handle)
}