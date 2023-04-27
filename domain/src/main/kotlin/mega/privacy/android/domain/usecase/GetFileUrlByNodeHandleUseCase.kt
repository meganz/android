package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for get local link by http server
 */
class GetFileUrlByNodeHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Get file url by node handle
     *
     * @param handle node handle
     * @return local link
     */
    suspend operator fun invoke(handle: Long) = mediaPlayerRepository.getFileUrlByNodeHandle(handle)
}