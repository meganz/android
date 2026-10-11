package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.HttpConnectionRepository
import java.net.URL
import javax.inject.Inject

/**
 * Get at most [maxBytes] bytes from the start of the given url using an HTTP Range request.
 */
class GetPartialDataBytesFromUrlUseCase @Inject constructor(
    private val httpConnectionRepository: HttpConnectionRepository,
) {
    /**
     * Invoke
     *
     * @param url the url to read from
     * @param maxBytes the maximum number of bytes to read from the start of the content
     */
    suspend operator fun invoke(url: URL, maxBytes: Int): ByteArray? =
        httpConnectionRepository.getDataBytesFromUrl(url, maxBytes)
}
