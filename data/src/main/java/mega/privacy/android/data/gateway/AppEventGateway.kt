package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow

internal interface AppEventGateway {

    /**
     * monitor upload service pause State
     */
    val monitorCameraUploadPauseState: Flow<Boolean>

    /**
     * monitor charging state
     */
    val monitorChargingStoppedState: Flow<Boolean>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()
}
