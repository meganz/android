package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Get audio node by handle use case
 */
class GetAudioNodeByHandleUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * Invoke
     *
     * @param handle
     * @param attemptFromFolderApi whether attempt from folder api
     */
    suspend operator fun invoke(handle: Long, attemptFromFolderApi: Boolean = false) =
        mediaPlayerRepository.getAudioNodeByHandle(handle, attemptFromFolderApi)
}