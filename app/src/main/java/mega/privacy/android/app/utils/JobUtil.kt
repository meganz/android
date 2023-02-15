package mega.privacy.android.app.utils

import android.content.Context
import android.os.Handler
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.jobservices.CancelCameraUploadWorker
import mega.privacy.android.app.jobservices.StartCameraUploadWorker
import mega.privacy.android.app.jobservices.StopCameraUploadWorker
import mega.privacy.android.app.jobservices.SyncHeartbeatCameraUploadWorker
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.domain.entity.StorageState
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Job Utility for Camera Upload
 */
object JobUtil {

    // when CU has nothing to upload, send up to date heartbeat every 30 minutes
    private const val UP_TO_DATE_HEARTBEAT_INTERVAL: Long = 30
    private const val HEARTBEAT_FLEX_INTERVAL: Long = 20 // 20 minutes
    private const val CU_SCHEDULER_INTERVAL: Long = 1 // 1 hour
    private const val SCHEDULER_FLEX_INTERVAL: Long = 50 // 50 minutes
    private const val CU_RESCHEDULE_INTERVAL: Long = 5000 // 5000 milliseconds
    private const val START_JOB_SUCCEED = 0
    private const val START_JOB_FAILED_NOT_ENABLED = -2

    /**
     * Ignore attributes intent data
     */
    const val SHOULD_IGNORE_ATTRIBUTES = "SHOULD_IGNORE_ATTRIBUTES"

    /**
     * Is primary handle sync done intent data
     */
    const val IS_PRIMARY_HANDLE_SYNC_DONE = "IS_PRIMARY_HANDLE_SYNC_DONE"

    /**
     * Worker tags
     */
    private const val CAMERA_UPLOAD_TAG = "CAMERA_UPLOAD_TAG"
    private const val SINGLE_CAMERA_UPLOAD_TAG = "MEGA_SINGLE_CAMERA_UPLOAD_TAG"
    private const val HEART_BEAT_TAG = "HEART_BEAT_TAG"
    private const val SINGLE_HEART_BEAT_TAG = "SINGLE_HEART_BEAT_TAG"
    private const val STOP_CAMERA_UPLOAD_TAG = "STOP_CAMERA_UPLOAD_TAG"
    private const val CANCEL_UPLOADS_TAG = "CANCEL_UPLOADS_TAG"

    /**
     * Schedule job of camera upload
     *
     * @param context From which the action is done.
     * @return The result of schedule job
     */
    @Synchronized
    fun scheduleCameraUploadJob(context: Context): Int {
        if (isCameraUploadDisabled) {
            Timber.d("scheduleCameraUploadJob() FAIL")
            return START_JOB_FAILED_NOT_ENABLED
        }
        scheduleCameraUploadSyncActiveHeartbeat(context)
        // periodic work that runs during the last 10 minutes of every one hour period
        val cameraUploadWorkRequest = PeriodicWorkRequest.Builder(
            StartCameraUploadWorker::class.java,
            CU_SCHEDULER_INTERVAL,
            TimeUnit.HOURS,
            SCHEDULER_FLEX_INTERVAL,
            TimeUnit.MINUTES
        )
            .addTag(CAMERA_UPLOAD_TAG)
            .setInputData(
                Data.Builder()
                    .putBoolean(SHOULD_IGNORE_ATTRIBUTES, false)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                CAMERA_UPLOAD_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cameraUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Schedule Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("scheduleCameraUploadJob() SUCCESS")
        return START_JOB_SUCCEED
    }

    /**
     * Schedule job of camera upload active heartbeat
     *
     * @param context From which the action is done.
     */
    private fun scheduleCameraUploadSyncActiveHeartbeat(context: Context) {
        // periodic work that runs during the last 10 minutes of every half an hour period
        val cuSyncActiveHeartbeatWorkRequest = PeriodicWorkRequest.Builder(
            SyncHeartbeatCameraUploadWorker::class.java,
            UP_TO_DATE_HEARTBEAT_INTERVAL,
            TimeUnit.MINUTES,
            HEARTBEAT_FLEX_INTERVAL,
            TimeUnit.MINUTES
        )
            .addTag(HEART_BEAT_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                HEART_BEAT_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                cuSyncActiveHeartbeatWorkRequest
            )
        Timber.d(
            "CameraUpload Schedule Heartbeat Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(HEART_BEAT_TAG)
            }"
        )
        Timber.d("scheduleCameraUploadSyncActiveHeartbeat() SUCCESS")
    }

    /**
     * Fire a single camera upload heartbeat
     *
     * @param context From which the action is done.
     */
    @Synchronized
    fun fireSingleHeartbeat(context: Context) {
        val heartbeatWorkRequest = OneTimeWorkRequest.Builder(
            SyncHeartbeatCameraUploadWorker::class.java
        )
            .addTag(SINGLE_HEART_BEAT_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SINGLE_HEART_BEAT_TAG,
                ExistingWorkPolicy.KEEP,
                heartbeatWorkRequest
            )
        Timber.d(
            "CameraUpload Single Heartbeat Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(SINGLE_HEART_BEAT_TAG)
            }"
        )
        Timber.d("fireSingleHeartbeat() SUCCESS")
    }

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of CameraUploadsService
     *
     * @param context From which the action is done.
     * @return The result of the job
     */
    @JvmStatic
    @Synchronized
    fun fireCameraUploadJob(
        context: Context,
        shouldIgnoreAttributes: Boolean,
        isPrimaryHandleSyncDone: Boolean = false,
    ): Int {
        if (isCameraUploadDisabled) {
            Timber.d("fireCameraUploadJob() FAIL")
            return START_JOB_FAILED_NOT_ENABLED
        }
        val cameraUploadWorkRequest = OneTimeWorkRequest.Builder(
            StartCameraUploadWorker::class.java
        )
            .addTag(SINGLE_CAMERA_UPLOAD_TAG)
            .setInputData(
                Data.Builder()
                    .putBoolean(SHOULD_IGNORE_ATTRIBUTES, shouldIgnoreAttributes)
                    .putBoolean(IS_PRIMARY_HANDLE_SYNC_DONE, isPrimaryHandleSyncDone)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SINGLE_CAMERA_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                cameraUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Single Job Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(SINGLE_CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("fireCameraUploadJob() SUCCESS")
        return START_JOB_SUCCEED
    }

    /**
     * Fire a request to stop camera upload service.
     *
     * @param context From which the action is done.
     */
    @JvmStatic
    @Synchronized
    fun fireStopCameraUploadJob(context: Context) {
        if (isCameraUploadDisabled) {
            Timber.d("fireStopCameraUploadJob() FAIL")
            return
        }
        val stopUploadWorkRequest = OneTimeWorkRequest.Builder(
            StopCameraUploadWorker::class.java
        )
            .addTag(STOP_CAMERA_UPLOAD_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                STOP_CAMERA_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                stopUploadWorkRequest
            )
        Timber.d(
            "CameraUpload Stop Job Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(STOP_CAMERA_UPLOAD_TAG)
            }"
        )
        Timber.d("fireStopCameraUploadJob() SUCCESS")
    }

    /**
     * Cancel all camera upload related jobs immediately, e.g. when all transfers are cancelled.
     *
     * @param context From which the action is done.
     */
    @JvmStatic
    @Synchronized
    fun fireCancelCameraUploadJob(context: Context) {
        val cancelWorkRequest = OneTimeWorkRequest.Builder(
            CancelCameraUploadWorker::class.java
        )
            .addTag(CANCEL_UPLOADS_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                CANCEL_UPLOADS_TAG,
                ExistingWorkPolicy.KEEP,
                cancelWorkRequest
            )
        Timber.d(
            "CameraUpload Cancel Job Work Status: ${
                WorkManager.getInstance(context).getWorkInfosByTag(CANCEL_UPLOADS_TAG)
            }"
        )
        Timber.d("fireCancelCameraUploadJob() SUCCESS")
    }

    /**
     * Reschedule Camera Upload with time interval
     */
    fun rescheduleCameraUpload(context: Context) {
        fireStopCameraUploadJob(context)
        Handler().postDelayed({
            scheduleCameraUploadJob(context)
        }, CU_RESCHEDULE_INTERVAL)
    }

    /**
     * Restart Camera Uploads by executing [StopCameraUploadWorker] and [StartCameraUploadWorker]
     * sequentially through Work Chaining
     *
     * @param context The Context to enqueue work
     * @param shouldIgnoreAttributes Whether to start Camera Uploads without checking User Attributes
     */
    @Synchronized
    fun fireRestartCameraUploadJob(context: Context, shouldIgnoreAttributes: Boolean) {
        val stopCameraUploadRequest = OneTimeWorkRequest.Builder(
            StopCameraUploadWorker::class.java
        )
            .addTag(STOP_CAMERA_UPLOAD_TAG)
            .build()
        val startCameraUploadRequest = OneTimeWorkRequest.Builder(
            StartCameraUploadWorker::class.java
        )
            .addTag(SINGLE_CAMERA_UPLOAD_TAG)
            .setInputData(
                Data.Builder()
                    .putBoolean(SHOULD_IGNORE_ATTRIBUTES, shouldIgnoreAttributes)
                    .putBoolean(IS_PRIMARY_HANDLE_SYNC_DONE, false)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .beginWith(stopCameraUploadRequest)
            .then(startCameraUploadRequest)
            .enqueue()
    }

    /**
     * Stop the camera upload work by tag.
     * Stop regular camera upload sync heartbeat work by tag.
     *
     * @param context From which the action is done.
     */
    fun stopCameraUploadSyncHeartbeatWorkers(context: Context) {
        val manager = WorkManager.getInstance(context)
        listOf(
            CAMERA_UPLOAD_TAG,
            SINGLE_CAMERA_UPLOAD_TAG,
            HEART_BEAT_TAG,
            SINGLE_HEART_BEAT_TAG
        ).forEach {
            manager.cancelAllWorkByTag(it)
        }
        Timber.d("stopCameraUploadSyncHeartbeatWorkers() SUCCESS")
    }

    private val isCameraUploadDisabled: Boolean
        get() {
            val prefs = getDbHandler().preferences
            if (prefs == null) {
                Timber.d("MegaPreferences not defined, so not enabled")
                return true
            }
            val cameraUploadEnabled = prefs.camSyncEnabled
            if (cameraUploadEnabled.isNullOrEmpty()) {
                Timber.d("CameraUpload not enabled")
                return true
            }
            return !cameraUploadEnabled.toBoolean()
        }

    /**
     * If storage over quota
     */
    val isOverQuota: Boolean
        get() = getStorageState() === StorageState.Red
}
