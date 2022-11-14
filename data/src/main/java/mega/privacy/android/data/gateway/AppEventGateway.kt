package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow

internal interface AppEventGateway {

    /**
     * monitor upload service pause State
     */
    val monitorCameraUploadPauseState: Flow<Boolean>


    /**
     * monitor battery info
     */
    val monitorBatteryInfo: Flow<Int>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()
}
