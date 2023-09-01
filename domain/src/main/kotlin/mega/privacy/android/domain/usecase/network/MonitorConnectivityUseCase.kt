package mega.privacy.android.domain.usecase.network

import kotlinx.coroutines.flow.map
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
    operator fun invoke() = networkRepository.monitorConnectivityChanges().map { it.connected }
}
