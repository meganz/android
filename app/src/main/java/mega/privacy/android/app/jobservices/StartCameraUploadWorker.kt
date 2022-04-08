package mega.privacy.android.app.jobservices

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.JobUtil.SHOULD_IGNORE_ATTRIBUTES
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import nz.mega.sdk.MegaApiJava

/**
 * Worker for upload images task
 */
class StartCameraUploadWorker(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (isStopped) return Result.failure()
        logDebug("CameraUploadWork: doWork()")
        val ignoreAttributes = inputData.getBoolean(SHOULD_IGNORE_ATTRIBUTES, false)
        return try {
            val isOverQuota =
                (appContext.applicationContext as MegaApplication).storageState == MegaApiJava.STORAGE_STATE_RED
            val hasReadPermission =
                hasPermissions(appContext, Manifest.permission.READ_EXTERNAL_STORAGE)
            logDebug(
                "isOverQuota: " + isOverQuota +
                        ", hasStoragePermission: " + hasReadPermission +
                        ", isRunning: " + CameraUploadsService.isServiceRunning +
                        ", should ignore attributes: " + ignoreAttributes
            )
            if (!CameraUploadsService.isServiceRunning && !isOverQuota && hasReadPermission) {
                val newIntent = Intent(appContext, CameraUploadsService::class.java)
                newIntent.putExtra(CameraUploadsService.EXTRA_IGNORE_ATTR_CHECK, ignoreAttributes)
                ContextCompat.startForegroundService(appContext, newIntent)
            }
            Result.success()
        } catch (throwable: Throwable) {
            logDebug("CameraUploadWork: doWork() fail")
            Result.failure()
        }
    }
}
