package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo

internal interface DeviceEventGateway {

    /**
     * monitor battery info
     */
    val monitorBatteryInfo: Flow<BatteryInfo>

    /**
     * monitor charging state
     */
    val monitorChargingStoppedState: Flow<Boolean>

    /**
     * Monitor muted chats
     */
    @Deprecated("App events need to be refactored to the new architecture. This gateway is limited to Device broadcasts")
    val monitorMutedChats: Flow<Boolean>
}
