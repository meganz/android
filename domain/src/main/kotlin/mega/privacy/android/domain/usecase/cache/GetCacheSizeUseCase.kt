package mega.privacy.android.domain.usecase.cache

import mega.privacy.android.domain.repository.CacheRepository
import javax.inject.Inject

/**
 * Get cache size use case
 */
class GetCacheSizeUseCase @Inject constructor(
    private val cacheRepository: CacheRepository
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = cacheRepository.getCacheSize()
}