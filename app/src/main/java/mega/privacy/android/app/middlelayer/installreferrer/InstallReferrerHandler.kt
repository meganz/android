package mega.privacy.android.app.middlelayer.installreferrer

/**
 * InstallReferrerHandler Interface
 */
interface InstallReferrerHandler {
    /**
     * Get Referrer Details
     */
    suspend fun getDetails(): InstallReferrerDetails
}