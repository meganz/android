package mega.privacy.android.app.utils.wrapper

import android.content.Context

/**
 * Is on WiFi wrapper
 *
 * Temporary wrapper interface
 */
interface IsOnWifiWrapper {
    fun isOnWifi(context: Context): Boolean
}
