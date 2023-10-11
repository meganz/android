package mega.privacy.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.camerauploads.SendCameraUploadsBackupHeartBeatUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendMediaUploadsBackupHeartBeatUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import timber.log.Timber

/**
 * Worker to send regular camera upload sync heart beats
 */
@HiltWorker
class SyncHeartbeatCameraUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val applicationWrapper: ApplicationWrapper,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val sendCameraUploadsBackupHeartBeatUseCase: SendCameraUploadsBackupHeartBeatUseCase,
    private val sendMediaUploadsBackupHeartBeatUseCase: SendMediaUploadsBackupHeartBeatUseCase,
    @LoginMutex private val loginMutex: Mutex,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (isStopped) return Result.failure()
        Timber.d("SyncHeartbeatCameraUploadWorker: doWork()")
        return try {
            // arbitrary retry value
            var retry = 3
            while (loginMutex.isLocked && retry > 0) {
                Timber.d("Wait for the isLoggingIn lock to be available")
                delay(1000)
                retry--
            }
            if (!loginMutex.isLocked) {
                backgroundFastLoginUseCase()
                Timber.d("backgroundFastLogin successful")
                applicationWrapper.setHeartBeatAlive(true)
                sendCameraUploadsBackupHeartBeatUseCase(
                    heartbeatStatus = HeartbeatStatus.UP_TO_DATE,
                    lastNodeHandle = -1L
                )
                Timber.d("Camera Uploads up to date heartbeat sent")
                sendMediaUploadsBackupHeartBeatUseCase(
                    heartbeatStatus = HeartbeatStatus.UP_TO_DATE,
                    lastNodeHandle = -1L
                )
                Timber.d("Media Uploads up to date heartbeat sent")
            }
            Result.success()
        } catch (throwable: Throwable) {
            Timber.e(throwable, "SyncHeartbeatCameraUploadWorker: doWork() fail")
            Result.failure()
        }
    }
}
