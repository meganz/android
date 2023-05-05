package mega.privacy.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import timber.log.Timber

/**
 * Worker to delete the oldest completed transfers
 */
@HiltWorker
internal class DeleteOldestCompletedTransfersWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        runCatching {
            megaLocalStorageGateway.deleteOldestCompletedTransfers()
        }.onFailure {
            Timber.e(it)
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        const val DELETE_OLDEST_TRANSFERS_WORKER_TAG = "DELETE_OLDEST_TRANSFERS_WORKER_TAG"
    }
}
