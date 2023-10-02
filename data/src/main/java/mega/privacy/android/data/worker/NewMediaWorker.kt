package mega.privacy.android.data.worker

import android.content.Context
import android.provider.MediaStore
import androidx.concurrent.futures.await
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import timber.log.Timber

@HiltWorker
internal class NewMediaWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        runCatching {
            if (isCameraUploadsEnabledUseCase()) {
                Timber.d("Capture new media")
                startCameraUploadUseCase()
                // it's one time job, we need to re-listen again
                scheduleWork(context, true)
            }
        }.onFailure {
            Timber.e(it)
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        private const val NEW_MEDIA_WORKER_TAG = "NEW_MEDIA_WORKER_TAG"
        suspend fun scheduleWork(context: Context, isForce: Boolean = false) {
            val workManager =
                WorkManager.getInstance(context.applicationContext)
            if (isForce
                || isQueuedOrRunning(workManager)
            ) {
                val photoCheckBuilder =
                    OneTimeWorkRequest.Builder(NewMediaWorker::class.java)
                photoCheckBuilder.setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true)
                        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                        .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
                        .addContentUriTrigger(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true)
                        .build()
                ).addTag(NEW_MEDIA_WORKER_TAG)
                workManager.enqueue(photoCheckBuilder.build())
            }
        }

        private suspend fun isQueuedOrRunning(workManager: WorkManager) =
            workManager.getWorkInfosByTag(NEW_MEDIA_WORKER_TAG)
                .await()
                .none { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }
}
