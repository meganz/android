package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Default monitor connectivity
 *
 * @property networkRepository
 */
class DefaultMonitorConnectivity @Inject constructor(private val networkRepository: NetworkRepository) :
    MonitorConnectivity {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(networkRepository.getCurrentConnectivityState().connected)
            emitAll(networkRepository.monitorConnectivityChanges().map { it.connected })
        }
    }
}