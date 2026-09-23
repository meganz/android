package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.WorkerClassGateway
import mega.privacy.android.data.worker.ChatUploadsWorker
import mega.privacy.android.data.worker.DeleteOldestCompletedTransfersWorker
import mega.privacy.android.data.worker.DownloadsWorker
import mega.privacy.android.data.worker.OfflineSyncWorker
import mega.privacy.android.data.worker.UploadsWorker
import javax.inject.Inject

/**
 * Implementation of [WorkerClassGateway]
 */
class WorkerClassGatewayImpl @Inject constructor() : WorkerClassGateway {

    override val downloadsWorkerClass = DownloadsWorker::class.java

    override val chatUploadsWorkerClass = ChatUploadsWorker::class.java

    override val deleteOldestCompletedTransferWorkerClass =
        DeleteOldestCompletedTransfersWorker::class.java

    override val uploadsWorkerClass = UploadsWorker::class.java

    override val offlineSyncWorkerClass = OfflineSyncWorker::class.java
}
