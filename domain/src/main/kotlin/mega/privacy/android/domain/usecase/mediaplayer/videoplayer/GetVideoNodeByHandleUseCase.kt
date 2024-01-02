package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Get video node by handle use case
 */
class GetVideoNodeByHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Invoke
     *
     * @param handle
     * @param attemptFromFolderApi whether attempt from folder api
     */
    suspend operator fun invoke(handle: Long, attemptFromFolderApi: Boolean = false) =
        mediaPlayerRepository.getVideoNodeByHandle(handle, attemptFromFolderApi)
}