package mega.privacy.android.app.jobservices

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import mega.privacy.android.app.utils.JobUtil.startCameraUploadService
import mega.privacy.android.app.utils.LogUtil

class CameraUploadWork(val appContext: Context, val workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        LogUtil.logDebug("startCameraUploadService")
        startCameraUploadService(appContext)

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
