package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApiFolder http server whether is running
 */
class MegaApiFolderHttpServerIsRunningUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * MegaApiFolder http server whether is running
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend operator fun invoke(): Int = mediaPlayerRepository.megaApiFolderHttpServerIsRunning()
}