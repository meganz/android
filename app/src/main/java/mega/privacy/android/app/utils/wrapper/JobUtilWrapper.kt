package mega.privacy.android.app.utils.wrapper

import android.content.Context
import mega.privacy.android.app.utils.JobUtil

/**
 * The interface for wrapping static [mega.privacy.android.app.utils.JobUtil] methods.
 */
interface JobUtilWrapper {

    /**
     * Wrapper method that calls [JobUtil.isOverQuota]
     *
     * @return Whether the account is Over quota
     */
    fun isOverQuota(): Boolean = JobUtil.isOverQuota

    /**
     * Wrapper method that calls [JobUtil.fireCameraUploadJob]
     *
     * @param context [Context]
     *
     * @return an Integer value
     */
    fun fireCameraUploadJob(context: Context): Int =
        JobUtil.fireCameraUploadJob(context)

    /**
     * Wrapper method that calls [JobUtil.fireStopCameraUploadJob]
     *
     * @param context [Context]
     */
    fun fireStopCameraUploadJob(context: Context) =
        JobUtil.fireStopCameraUploadJob(context)

    /**
     * Wrapper method that calls [JobUtil.fireRestartCameraUploadJob]
     *
     * @param context [Context]
     */
    fun fireRestartCameraUploadJob(context: Context) =
        JobUtil.fireRestartCameraUploadJob(context)

    /**
     * Wrapper method that calls [JobUtil.stopCameraUploadSyncHeartbeatWorkers]
     *
     * @param context [Context]
     */
    fun stopCameraUploadSyncHeartbeatWorkers(context: Context) =
        JobUtil.stopCameraUploadSyncHeartbeatWorkers(context)
}
