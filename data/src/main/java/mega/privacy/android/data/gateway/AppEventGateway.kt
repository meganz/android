package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo

internal interface AppEventGateway {

    /**
     * monitor upload service pause State
     */
    val monitorCameraUploadPauseState: Flow<Boolean>


    /**
     * monitor battery info
     */
    val monitorBatteryInfo: Flow<BatteryInfo>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()
}
