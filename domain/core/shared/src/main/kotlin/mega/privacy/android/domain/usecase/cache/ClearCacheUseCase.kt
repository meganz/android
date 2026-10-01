package mega.privacy.android.domain.usecase.cache

import mega.privacy.android.domain.repository.CacheRepository
import javax.inject.Inject

/**
 * Clear Cache use case
 */
class ClearCacheUseCase @Inject constructor(
    private val cacheRepository: CacheRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = cacheRepository.clearCache()
}