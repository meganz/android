package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApi http server starts
 */
class MegaApiHttpServerStartUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * MegaApi http server starts
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend operator fun invoke(): Boolean = mediaPlayerRepository.megaApiHttpServerStart()
}