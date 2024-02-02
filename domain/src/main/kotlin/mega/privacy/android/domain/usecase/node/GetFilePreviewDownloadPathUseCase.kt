package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.CacheRepository
import javax.inject.Inject

/**
 * Use case to get the path to download file for preview
 *
 * @property cacheRepository Repository to provide the data
 */
class GetFilePreviewDownloadPathUseCase @Inject constructor(
    private val cacheRepository: CacheRepository,
) {
    /**
     * Invoke the use case
     *
     * @return the path to download file for preview
     */
    suspend operator fun invoke() = cacheRepository.getPreviewDownloadPathForNode()
}