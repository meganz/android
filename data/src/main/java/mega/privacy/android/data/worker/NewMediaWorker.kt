package mega.privacy.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import timber.log.Timber

/**
 * Worker to listen to new media captured.
 */
@HiltWorker
class NewMediaWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        runCatching {
            Timber.d("New media captured. Start camera upload worker.")
            startCameraUploadUseCase()
            listenToNewMediaUseCase(forceEnqueue = true)
        }.onFailure {
            Timber.e(it)
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        /**
         * Tag identifying the worker when enqueued
         *
         */
        const val NEW_MEDIA_WORKER_TAG = "NEW_MEDIA_WORKER_TAG"
    }
}
