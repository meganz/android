package mega.privacy.android.data.facade

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import dagger.Lazy
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.WorkerClassGateway
import mega.privacy.android.data.worker.ChatUploadsWorker
import mega.privacy.android.data.worker.DeleteOldestCompletedTransfersWorker
import mega.privacy.android.data.worker.DownloadsWorker
import mega.privacy.android.data.worker.OfflineSyncWorker.Companion.OFFLINE_SYNC_WORKER_TAG
import mega.privacy.android.data.worker.UploadsWorker
import mega.privacy.android.domain.monitoring.CrashReporter
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Responsible of managing the queue of workers in the WorkManager
 */
class WorkManagerGatewayImpl @Inject constructor(
    private val workManager: Lazy<WorkManager>,
    private val crashReporter: CrashReporter,
    private val workerClassGateway: WorkerClassGateway,
) : WorkManagerGateway {

    override suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest() {
        workManager.get().debugWorkInfo(crashReporter)

        val workRequest =
            OneTimeWorkRequest.Builder(workerClassGateway.deleteOldestCompletedTransferWorkerClass)
                .addTag(DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG)
                .setInitialDelay(1.minutes.toJavaDuration()) // let's wait to don't affect performance on app start-up
                .build()

        workManager.get()
            .enqueueUniqueWork(
                DeleteOldestCompletedTransfersWorker.DELETE_OLDEST_TRANSFERS_WORKER_TAG,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    override suspend fun enqueueDownloadsWorkerRequest() {
        workManager.get().debugWorkInfo(crashReporter)

        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.downloadsWorkerClass)
                .addTag(DownloadsWorker.SINGLE_DOWNLOAD_TAG)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        workManager.get()
            .enqueueUniqueWork(
                DownloadsWorker.SINGLE_DOWNLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    override suspend fun enqueueChatUploadsWorkerRequest() {
        workManager.get().debugWorkInfo(crashReporter)

        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.chatUploadsWorkerClass)
                .addTag(ChatUploadsWorker.SINGLE_CHAT_UPLOAD_TAG)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        workManager.get()
            .enqueueUniqueWork(
                ChatUploadsWorker.SINGLE_CHAT_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    override fun monitorDownloadsStatusInfo() =
        workManager.get().getWorkInfosByTagFlow(DownloadsWorker.SINGLE_DOWNLOAD_TAG)

    override fun monitorChatUploadsStatusInfo() =
        workManager.get().getWorkInfosByTagFlow(ChatUploadsWorker.SINGLE_CHAT_UPLOAD_TAG)

    override suspend fun enqueueUploadsWorkerRequest() {
        workManager.get().debugWorkInfo(crashReporter)

        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.uploadsWorkerClass)
                .addTag(UploadsWorker.SINGLE_UPLOAD_TAG)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        workManager.get()
            .enqueueUniqueWork(
                UploadsWorker.SINGLE_UPLOAD_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    override fun monitorUploadsStatusInfo() =
        workManager.get().getWorkInfosByTagFlow(UploadsWorker.SINGLE_UPLOAD_TAG)

    override suspend fun startOfflineSync() {
        workManager.get().debugWorkInfo(crashReporter)
        val request =
            OneTimeWorkRequest.Builder(workerClassGateway.offlineSyncWorkerClass)
                .addTag(OFFLINE_SYNC_WORKER_TAG)
                .build()
        workManager.get()
            .enqueueUniqueWork(
                OFFLINE_SYNC_WORKER_TAG,
                ExistingWorkPolicy.KEEP,
                request
            )
    }
}

/**
 * Prints the list of pending [WorkInfo] in the log.
 */
suspend fun WorkManager.debugWorkInfo(crashReporter: CrashReporter) {
    getWorkInfosFlow(
        WorkQuery.fromStates(
            WorkInfo.State.ENQUEUED,
        )
    ).firstOrNull()
        ?.map { it.tags }
        ?.groupBy { it }
        ?.mapValues { it.value.size }
        ?.let {
            Timber.d("Worker pending list: $it")
            crashReporter.log("Worker pending list: $it")
        }
        ?: Timber.d("Worker pending list: empty")
}
