package mega.privacy.android.domain.usecase.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Use case for monitoring connectivity.
 *
 */
@Singleton
class MonitorConnectivityUseCase @Inject constructor(
    networkRepository: NetworkRepository,
    @ApplicationScope appScope: CoroutineScope,
) {

    private val flow = networkRepository.monitorConnectivityChanges().map { it.connected }
        .stateIn(
            appScope,
            SharingStarted.Eagerly,
            networkRepository.getCurrentConnectivityState().connected,
        )

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = flow
}
