package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Camera Upload preferences gateway
 */
interface CameraUploadPreferencesGateway {

    /**
     * Is camera upload service running
     *
     * @return true, if camera upload is running
     */
    fun isCameraUploadRunning(): Flow<Boolean>

    /**
     * Set camera upload running flag
     *
     * @return
     */
    suspend fun setIsCameraUploadRunning(isRunning: Boolean)

    /**
     * Clears camera upload preferences
     */
    suspend fun clearPreferences()
}
