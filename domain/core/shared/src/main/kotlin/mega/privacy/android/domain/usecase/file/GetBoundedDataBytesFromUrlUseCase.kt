package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.HttpConnectionRepository
import java.net.URL
import javax.inject.Inject

/**
 * Get the full content of the given url, refusing content larger than [maxBytes] instead of
 * silently truncating it.
 */
class GetBoundedDataBytesFromUrlUseCase @Inject constructor(
    private val httpConnectionRepository: HttpConnectionRepository,
) {
    /**
     * Invoke
     *
     * @param url the url to read from
     * @param maxBytes the largest content size that is accepted
     * @return the content, or null when the url could not be read
     * @throws mega.privacy.android.domain.exception.DataTooLargeException when the content is
     * larger than [maxBytes]
     */
    suspend operator fun invoke(url: URL, maxBytes: Int): ByteArray? {
        require(maxBytes > 0) { "maxBytes must be positive" }
        return httpConnectionRepository.getDataBytesFromUrlWithinLimit(url, maxBytes)
    }
}
