package mega.privacy.android.domain.repository

/**
 * Cache File Repository
 */
interface CacheFileRepository {

    /**
     * clean files from Cache directory
     */
    suspend fun purgeCacheDirectory()
}