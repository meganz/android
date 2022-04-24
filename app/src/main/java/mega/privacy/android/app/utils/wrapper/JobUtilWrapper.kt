package mega.privacy.android.app.utils.wrapper

import android.content.Context

/**
 * The interface for wrapping static [mega.privacy.android.app.utils.JobUtil] methods.
 */
interface JobUtilWrapper {
    fun isOverQuota(context: Context): Boolean
}
