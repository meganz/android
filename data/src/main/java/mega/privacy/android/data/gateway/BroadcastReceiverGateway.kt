package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo

internal interface BroadcastReceiverGateway {

    /**
     * monitor battery info
     */
    val monitorBatteryInfo: Flow<BatteryInfo>
}
