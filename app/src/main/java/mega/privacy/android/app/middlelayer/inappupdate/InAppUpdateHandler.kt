package mega.privacy.android.app.middlelayer.inappupdate

/**
 * InAppUpdate Handler
 */
interface InAppUpdateHandler {
    /**
     * Check for App Updates
     */
    suspend fun checkForAppUpdates()

    /**
     * Check for App Update Installation Status
     */
    suspend fun checkForInAppUpdateInstallStatus()

    /**
     * Complete the update
     */
    fun completeUpdate()
}