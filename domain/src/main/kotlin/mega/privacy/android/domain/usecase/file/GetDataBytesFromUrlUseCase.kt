package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.HttpConnectionRepository
import java.net.URL
import javax.inject.Inject

/**
 * Get bytes of data from the given url
 */
class GetDataBytesFromUrlUseCase @Inject constructor(
    private val httpConnectionRepository: HttpConnectionRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(url: URL): ByteArray? =
        httpConnectionRepository.getDataBytesFromUrl(url)
}