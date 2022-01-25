package mega.privacy.android.app.utils

import nz.mega.sdk.MegaApiAndroid

/**
 * Generic static methods to simplify MegaApi calls
 */
object MegaApiUtil {

    /**
     * Check if the MegaApi object is logged in.
     *
     * @return  True if it's logged in, false otherwise
     */
    @JvmStatic
    fun MegaApiAndroid.isUserLoggedIn(): Boolean =
            isLoggedIn > 0
}
