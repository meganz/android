package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor battery level State
 */
fun interface MonitorBatteryLevelState {
    /**
     * Invoke
     *
     * @return flow of event
     */
    operator fun invoke(): Flow<Int>
}
