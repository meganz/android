package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApi http server stop
 */
class MegaApiHttpServerStopUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * MegaApi http server stop
     */
    suspend operator fun invoke() = mediaPlayerRepository.megaApiHttpServerStop()
}