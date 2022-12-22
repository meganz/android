package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor charging stopped State
 */
fun interface MonitorChargingStoppedState {
    /**
     * Invoke
     *
     * @return flow of event
     */
    operator fun invoke(): Flow<Boolean>
}
