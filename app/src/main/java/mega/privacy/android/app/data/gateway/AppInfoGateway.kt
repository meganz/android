package mega.privacy.android.app.data.gateway

/**
 * App gate way
 *
 * @constructor Create empty App gate way
 */
interface AppInfoGateway {
    /**
     * Get app version code
     *
     */
    fun getAppVersionCode(): Int
}