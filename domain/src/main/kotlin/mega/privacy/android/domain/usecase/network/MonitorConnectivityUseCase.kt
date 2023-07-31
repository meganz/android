package mega.privacy.android.domain.usecase.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject


/**
 * Use case for monitoring connectivity.
 *
 */
class MonitorConnectivityUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = networkRepository.monitorConnectivityChanges().map { it.connected }
        .stateIn(
            appScope,
            SharingStarted.Eagerly,
            networkRepository.getCurrentConnectivityState().connected,
        )
}
