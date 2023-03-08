package mega.privacy.android.app.jobservices

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.data.wrapper.JobUtilWrapper
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
) :
    Worker(appContext, workerParams) {

    /**
     * Start camera upload process if all conditions are fulfilled
     */
    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("CameraUploadWork: doWork()")
        return try {
            val isOverQuota = jobUtilWrapper.isOverQuota()
            val hasMediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionUtilWrapper.hasPermissions(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                permissionUtilWrapper.hasPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            }
            Timber.d("isOverQuota: $isOverQuota, hasMediaPermissions: $hasMediaPermissions")
            if (!isOverQuota && hasMediaPermissions) {
                val newIntent = Intent(appContext, CameraUploadsService::class.java)
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
