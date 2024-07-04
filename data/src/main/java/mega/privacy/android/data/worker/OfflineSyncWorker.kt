package mega.privacy.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.domain.usecase.offline.SyncOfflineFilesUseCase
import timber.log.Timber

/**
 * Worker to sync offline local file with database
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncOfflineFilesUseCase: SyncOfflineFilesUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        runCatching {
            syncOfflineFilesUseCase()
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
        const val OFFLINE_SYNC_WORKER_TAG = "OFFLINE_SYNC_WORKER_TAG"
    }
}
