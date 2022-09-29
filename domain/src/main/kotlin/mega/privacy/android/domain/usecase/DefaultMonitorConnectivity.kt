package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default monitor connectivity
 *
 * @property networkRepository
 */
@Singleton
class DefaultMonitorConnectivity @Inject constructor(
    private val networkRepository: NetworkRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : MonitorConnectivity {
    override fun invoke(): StateFlow<Boolean> =
        networkRepository.monitorConnectivityChanges().map { it.connected }
            .stateIn(
                appScope,
                SharingStarted.Eagerly,
                networkRepository.getCurrentConnectivityState().connected,
            )
}