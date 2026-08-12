package mega.privacy.android.feature.sync.domain.usecase.sync.option

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for determining if sync should be allowed based on all conditions:
 * internet connectivity, WiFi settings, battery level, charging state,
 * and power save mode (battery saver)
 */
class MonitorShouldSyncUseCase @Inject constructor(
    private val monitorSyncPauseReasonUseCase: MonitorSyncPauseReasonUseCase,
) {

    /**
     * Determines if sync should be allowed based on all conditions including internet connectivity,
     * WiFi settings, battery level, charging state, and power save mode (battery saver)
     *
     * @return Flow<Boolean> indicating if sync should be allowed
     */
    operator fun invoke(): Flow<Boolean> = monitorSyncPauseReasonUseCase().map { it == null }
}
