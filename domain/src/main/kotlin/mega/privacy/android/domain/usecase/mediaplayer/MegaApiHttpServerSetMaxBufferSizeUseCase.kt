package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApi sets the maximum buffer size for the internal buffer
 */
class MegaApiHttpServerSetMaxBufferSizeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * MegaApi sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend operator fun invoke(bufferSize: Int) =
        mediaPlayerRepository.megaApiHttpServerSetMaxBufferSize(bufferSize)
}