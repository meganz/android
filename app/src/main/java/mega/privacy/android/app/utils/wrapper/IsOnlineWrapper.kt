package mega.privacy.android.app.utils.wrapper

import android.content.Context

/**
 * Is online wrapper
 *
 * Temporary wrapper interface
 */
interface IsOnlineWrapper {
    fun isOnline(context: Context): Boolean
}