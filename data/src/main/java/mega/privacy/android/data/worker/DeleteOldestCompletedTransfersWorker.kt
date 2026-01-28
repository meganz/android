package mega.privacy.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.domain.usecase.transfers.active.UpdateActiveTransfersAndCleanGroupsUseCase
import timber.log.Timber

/**
 * Worker to delete the oldest completed transfers
 */
@HiltWorker
class DeleteOldestCompletedTransfersWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val updateActiveTransfersAndCleanGroupsUseCase: UpdateActiveTransfersAndCleanGroupsUseCase,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        if (listOf<suspend () -> Unit>(
                { megaLocalRoomGateway.migrateLegacyCompletedTransfers() },
                { updateActiveTransfersAndCleanGroupsUseCase() },
                { megaLocalRoomGateway.deleteOldestCompletedTransfers() },
            ).map {
                runCatching { it() }.onFailure { e -> Timber.e(e) }
            }.all { it.isSuccess }
        ) {
            Result.success()
        } else {
            Result.failure()
        }

    companion object {
        const val DELETE_OLDEST_TRANSFERS_WORKER_TAG = "DELETE_OLDEST_TRANSFERS_WORKER_TAG"
    }
}
