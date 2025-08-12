package mega.privacy.android.app.utils

/**
 * For storing all the fixed urls.
 */
object ConstantsUrl {

    /**
     * MEGA website url
     */
    fun megaUrl(domainName: String) = "https://$domainName/"

    /**
     * Url for accessing account recovery page.
     */
    fun recoveryUrl(domainName: String) = "https://$domainName/recovery"

    /**
     * Url for accessing account recovery page with email parameter.
     */
    fun recoveryUrlEmail(domainName: String) = "https://$domainName/recovery?email="
}