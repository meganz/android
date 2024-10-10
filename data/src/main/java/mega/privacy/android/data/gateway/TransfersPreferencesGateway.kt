package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow

/**
 * Gateway to manage the transfers preferences
 */
interface TransfersPreferencesGateway {

    /**
     * Sets whether the user has denied the file access permission request
     */
    suspend fun setRequestFilesPermissionDenied()

    /**
     * Monitors whether the user has denied the file access permission request
     */
    fun monitorRequestFilesPermissionDenied(): Flow<Boolean>

    /**
     * Clear all preferences
     */
    suspend fun clearPreferences()
}