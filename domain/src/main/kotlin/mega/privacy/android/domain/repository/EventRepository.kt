package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow

interface EventRepository {

    /**
     * monitor upload service pause State
     */
    fun monitorCameraUploadPauseState(): Flow<Boolean>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()
}
