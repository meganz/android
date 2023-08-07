package mega.privacy.android.app.middlelayer.inappupdate

/**
 * InAppUpdate Handler
 */
interface InAppUpdateHandler {
    /**
     * Check for App Updates
     */
    suspend fun checkForAppUpdates(): Boolean

    /**
     * Complete the update
     */
    fun completeUpdate()
}