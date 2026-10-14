package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApiFolder http server stop
 */
class MegaApiFolderHttpServerStopUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * MegaApiFolder http server stop
     */
    suspend operator fun invoke() = mediaPlayerRepository.megaApiFolderHttpServerStop()
}