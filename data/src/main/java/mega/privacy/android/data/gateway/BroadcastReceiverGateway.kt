package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow

internal interface BroadcastReceiverGateway {

    /**
     * monitor upload service pause State
     */
    val monitorCameraUploadPauseState: Flow<Boolean>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()
}