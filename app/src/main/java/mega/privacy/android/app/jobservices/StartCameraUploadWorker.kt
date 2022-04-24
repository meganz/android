package mega.privacy.android.app.jobservices

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.utils.JobUtil.SHOULD_IGNORE_ATTRIBUTES
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import javax.inject.Inject

/**
 * Worker for upload images task
 */
class StartCameraUploadWorker @Inject constructor(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val permissionUtilWrapper: PermissionUtilWrapper,
    private val jobUtilWrapper: JobUtilWrapper,
    private val cameraUploadsServiceWrapper: CameraUploadsServiceWrapper
) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        logDebug("CameraUploadWork: doWork()")
        val ignoreAttributes = inputData.getBoolean(SHOULD_IGNORE_ATTRIBUTES, false)
        return try {
            val isOverQuota = jobUtilWrapper.isOverQuota(appContext)
            val hasReadPermission =
                permissionUtilWrapper.hasPermissions(
                    appContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            logDebug(
                "isOverQuota: " + isOverQuota +
                        ", hasStoragePermission: " + hasReadPermission +
                        ", isRunning: " + cameraUploadsServiceWrapper.isServiceRunning() +
                        ", should ignore attributes: " + ignoreAttributes
            )
            if (!cameraUploadsServiceWrapper.isServiceRunning() && !isOverQuota && hasReadPermission) {
                val newIntent = Intent(appContext, CameraUploadsService::class.java)
                newIntent.putExtra(CameraUploadsService.EXTRA_IGNORE_ATTR_CHECK, ignoreAttributes)
                ContextCompat.startForegroundService(appContext, newIntent)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (throwable: Throwable) {
            logDebug("CameraUploadWork: doWork() fail")
            Result.failure()
        }
    }
}
