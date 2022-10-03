package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CacheFileRepository
import javax.inject.Inject

/**
 * Default implementation of [CacheFileRepository]
 *
 * @property cacheFolderGateway CacheFolderGateway
 * @property ioDispatcher CoroutineDispatcher
 */
class DefaultCacheFileRepository @Inject constructor(
    private val cacheFolderGateway: CacheFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CacheFileRepository {

    override suspend fun purgeCacheDirectory() = withContext(ioDispatcher) {
        cacheFolderGateway.purgeCacheDirectory()
    }
}