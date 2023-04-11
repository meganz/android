package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApiFolder http server starts
 */
class MegaApiFolderHttpServerStartUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * MegaApiFolder http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend operator fun invoke(): Boolean = mediaPlayerRepository.megaApiFolderHttpServerStart()
}