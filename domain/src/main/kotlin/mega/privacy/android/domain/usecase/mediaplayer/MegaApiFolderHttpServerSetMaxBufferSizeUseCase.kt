package mega.privacy.android.domain.usecase.mediaplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for MegaApiFolder sets the maximum buffer size for the internal buffer
 */
class MegaApiFolderHttpServerSetMaxBufferSizeUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {

    /**
     * MegaApiFolder sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend operator fun invoke(bufferSize: Int) =
        mediaPlayerRepository.megaApiFolderHttpServerSetMaxBufferSize(bufferSize)
}