package mega.privacy.android.domain.repository

/**
 * Cache Repository
 */
interface CacheRepository {
    /**
     * Get Cache Size
     */
    suspend fun getCacheSize(): Long

    /**
     * Clear Cache
     */
    suspend fun clearCache()
}