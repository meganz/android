package mega.privacy.android.app.utils.wrapper

import android.content.Context
import mega.privacy.android.app.utils.JobUtil

/**
 * The interface for wrapping static [mega.privacy.android.app.utils.JobUtil] methods.
 */
interface JobUtilWrapper {

    fun isOverQuota(): Boolean = JobUtil.isOverQuota()

    fun fireCameraUploadJob(context: Context, shouldIgnoreAttributes: Boolean): Int =
        JobUtil.fireCameraUploadJob(context, shouldIgnoreAttributes)

    fun fireStopCameraUploadJob(context: Context) =
        JobUtil.fireStopCameraUploadJob(context)

}
