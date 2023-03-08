package mega.privacy.android.data.wrapper

import android.content.Context

/**
 * The interface for wrapping static [mega.privacy.android.app.utils.JobUtil] methods.
 */
interface JobUtilWrapper {

    /**
     * Wrapper method that calls [JobUtil.isOverQuota]
     *
     * @return Whether the account is Over quota
     */
    fun isOverQuota(): Boolean

    /**
     * Wrapper method that calls [JobUtil.fireCameraUploadJob]
     *
     * @param context [Context]
     *
     * @return an Integer value
     */
    fun fireCameraUploadJob(context: Context): Int

    /**
     * Wrapper method that calls [JobUtil.fireStopCameraUploadJob]
     *
     * @param context [Context]
     */
    fun fireStopCameraUploadJob(context: Context)

    /**
     * Wrapper method that calls [JobUtil.fireRestartCameraUploadJob]
     *
     * @param context [Context]
     */
    fun fireRestartCameraUploadJob(context: Context)

    /**
     * Wrapper method that calls [JobUtil.stopCameraUploadSyncHeartbeatWorkers]
     *
     * @param context [Context]
     */
    fun stopCameraUploadSyncHeartbeatWorkers(context: Context)
}
