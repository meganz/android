package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor camera upload pause state
 */
fun interface MonitorCameraUploadPauseState {
    /**
     * Invoke
     *
     * @return flow of event
     */
    operator fun invoke(): Flow<Boolean>
}