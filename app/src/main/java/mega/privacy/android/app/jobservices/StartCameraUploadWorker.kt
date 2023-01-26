package mega.privacy.android.app.jobservices

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.app.jobservices.CameraUploadsService.Companion.EXTRA_IGNORE_ATTR_CHECK
import mega.privacy.android.app.jobservices.CameraUploadsService.Companion.EXTRA_PRIMARY_SYNC_SUCCESS
import mega.privacy.android.app.utils.JobUtil.IS_PRIMARY_HANDLE_SYNC_DONE
import mega.privacy.android.app.utils.JobUtil.SHOULD_IGNORE_ATTRIBUTES
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import timber.log.Timber

/**
 * Worker for upload images task.
 * Starts if:
 * 1. The service is not already running
 * 2. The storage is not over quota
 * 3. The app has denied the Media Permissions
 */
@HiltWorker
class StartCameraUploadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val permissionUtilWrapper: PermissionUtilWrapper,
    private val jobUtilWrapper: JobUtilWrapper,
    private val cameraUploadsServiceWrapper: CameraUploadsServiceWrapper,
) :
    Worker(appContext, workerParams) {

    /**
     * Start camera upload process if all conditions are fulfilled
     */
    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("CameraUploadWork: doWork()")
        val ignoreAttributes = inputData.getBoolean(SHOULD_IGNORE_ATTRIBUTES, false)
        val isPrimaryHandleSynced = inputData.getBoolean(IS_PRIMARY_HANDLE_SYNC_DONE, false)
        return try {
            val isOverQuota = jobUtilWrapper.isOverQuota()
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    getNotificationsPermission(),
                    getImagePermissionByVersion(),
                    getVideoPermissionByVersion()
                )
            } else {
                arrayOf(
                    getImagePermissionByVersion(),
                    getVideoPermissionByVersion()
                )
            }
            val hasMediaPermissions = permissionUtilWrapper.hasPermissions(appContext, *permissions)
            Timber.d("isOverQuota: $isOverQuota, hasMediaPermissions: $hasMediaPermissions, isRunning: ${cameraUploadsServiceWrapper.isServiceRunning()}, ignoreAttributes: $ignoreAttributes")
            if (!cameraUploadsServiceWrapper.isServiceRunning() && !isOverQuota && hasMediaPermissions) {
                val newIntent = Intent(appContext, CameraUploadsService::class.java)
                newIntent.putExtra(EXTRA_IGNORE_ATTR_CHECK, ignoreAttributes)
                newIntent.putExtra(EXTRA_PRIMARY_SYNC_SUCCESS, isPrimaryHandleSynced)
                ContextCompat.startForegroundService(appContext, newIntent)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (throwable: Throwable) {
            Timber.d("CameraUploadWork: doWork() fail")
            Result.failure()
        }
    }
}
