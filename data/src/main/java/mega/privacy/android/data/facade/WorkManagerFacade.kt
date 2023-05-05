package mega.privacy.android.data.facade

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.worker.DeleteOldestCompletedTransfersWorker
import javax.inject.Inject

internal class WorkManagerFacade @Inject constructor(
    private val workManager: WorkManager
) : WorkManagerGateway {

    override suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest() {
        val workRequest =
            OneTimeWorkRequest.Builder(DeleteOldestCompletedTransfersWorker::class.java)
                .addTag(DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG)
                .build()

        workManager
            .enqueueUniqueWork(
                DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }
}
