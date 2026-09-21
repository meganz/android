package mega.privacy.android.domain.usecase.network

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject


/**
 * Use case for monitoring connectivity.
 *
 */
class MonitorConnectivityUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = networkRepository.monitorConnectivityChanges()
        .onStart { emit(networkRepository.getCurrentConnectivityState()) }.map { it.connected }
}
