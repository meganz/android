package mega.privacy.android.app.utils.wrapper

import android.content.Context
import mega.privacy.android.app.utils.JobUtil

/**
 * The interface for wrapping static [mega.privacy.android.app.utils.JobUtil] methods.
 */
interface JobUtilWrapper {
    fun isOverQuota(context: Context): Boolean = JobUtil.isOverQuota(context)
    fun fireCameraUploadJob(context: Context, shouldIgnoreAttributes: Boolean): Int =
        JobUtil.fireCameraUploadJob(context, shouldIgnoreAttributes)
}
