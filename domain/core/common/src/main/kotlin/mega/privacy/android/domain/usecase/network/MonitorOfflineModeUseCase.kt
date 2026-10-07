package mega.privacy.android.domain.usecase.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Monitors whether the app is in offline mode, meaning content that needs the server cannot load.
 */
class MonitorOfflineModeUseCase @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) {

    /**
     * Invoke.
     *
     * @return Flow emitting true while the app is in offline mode
     */
    operator fun invoke(): Flow<Boolean> =
        monitorConnectivityUseCase()
            .map { isConnected -> !isConnected }
            .distinctUntilChanged()
}
