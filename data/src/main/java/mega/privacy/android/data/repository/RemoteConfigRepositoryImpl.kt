package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.RemoteConfigGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RemoteConfigRepository
import javax.inject.Inject

internal class RemoteConfigRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteConfigGateway: RemoteConfigGateway,
) : RemoteConfigRepository {

    private val fetchMutex = Mutex()

    override suspend fun fetchAndActivate(useMinimalFetchInterval: Boolean): Boolean =
        withContext(ioDispatcher) {
            fetchMutex.withLock {
                if (useMinimalFetchInterval) {
                    remoteConfigGateway.setMinimumFetchInterval(0)
                }
                remoteConfigGateway.fetchAndActivate()
            }
        }
}
