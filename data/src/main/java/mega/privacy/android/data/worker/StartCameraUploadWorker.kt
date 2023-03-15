package mega.privacy.android.data.worker

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.wrapper.CameraUploadServiceWrapper
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import timber.log.Timber

/**
 * Worker for upload images task.
 * Starts if:
 * 1. The storage has enough quota left
 * 2. The app has given media permissions
 */
@HiltWorker
class StartCameraUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cameraUploadServiceWrapper: CameraUploadServiceWrapper,
    private val permissionsGateway: PermissionGateway,
    private val isNotEnoughQuota: IsNotEnoughQuota,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("StartCameraUploadWorker: Working")
        return try {
            val isNotEnoughQuota = isNotEnoughQuota()
            val hasMediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsGateway.hasPermissions(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                permissionsGateway.hasPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            }
            Timber.d("isNotEnoughQuota: $isNotEnoughQuota, hasMediaPermissions: $hasMediaPermissions")
            if (!isNotEnoughQuota && hasMediaPermissions) {
                val newIntent = cameraUploadServiceWrapper.newIntent(context)
                ContextCompat.startForegroundService(context, newIntent)
                Timber.d("StartCameraUploadWorker: Finished with Success")
                Result.success()
            } else {
                Timber.d("StartCameraUploadWorker: Finished with Failure")
                Result.failure()
            }
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            Result.failure()
        }
    }
}
