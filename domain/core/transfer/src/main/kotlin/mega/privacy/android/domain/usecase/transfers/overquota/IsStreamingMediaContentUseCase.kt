package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.StreamingServerRepository
import javax.inject.Inject

/**
 * Tells whether the content streamed when a bandwidth over quota was hit is media, so the warning
 * can be phrased as interrupted playback rather than an interrupted file read.
 */
class IsStreamingMediaContentUseCase @Inject constructor(
    private val streamingServerRepository: StreamingServerRepository,
) {

    /**
     * @return true when the last streaming request was for audio or video.
     */
    operator fun invoke(): Boolean = streamingServerRepository.isLastStreamedContentMedia()
}
