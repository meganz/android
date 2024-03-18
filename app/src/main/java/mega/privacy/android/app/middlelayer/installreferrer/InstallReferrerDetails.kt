package mega.privacy.android.app.middlelayer.installreferrer

/**
 * Install Referrer Details
 *
 * @property referrerUrl The referrer URL
 * @property referrerClickTime The referrer click time
 * @property appInstallTime The app install time
 */
data class InstallReferrerDetails(
    val referrerUrl: String?,
    val referrerClickTime: Long?,
    val appInstallTime: Long?,
)