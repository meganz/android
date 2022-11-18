package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo

/**
 * Monitor battery Info
 */
fun interface MonitorBatteryInfo {
    /**
     * Invoke
     *
     * @return flow of event
     */
    operator fun invoke(): Flow<BatteryInfo>
}
