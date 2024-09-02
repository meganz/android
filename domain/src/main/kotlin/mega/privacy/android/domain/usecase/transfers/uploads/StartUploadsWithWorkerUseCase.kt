package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractStartTransfersWithWorkerUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case for start uploading a list of files and folders to a Cloud location and returns a Flow
 * to monitor the progress until the files are scanned. While the returned flow is not completed
 * the app should be blocked to avoid other interaction with the sdk to avoid issues.
 * Once the flow is completed the sdj will keep uploading and UploadsWorker will monitor updates globally.
 * If cancelled before completion, the processing will be cancelled.
 */
class StartUploadsWithWorkerUseCase @Inject constructor(
    private val uploadFilesUseCase: UploadFilesUseCase,
    private val startUploadsWorkerAndWaitUntilIsStartedUseCase: StartUploadsWorkerAndWaitUntilIsStartedUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getFileForUploadUseCase: GetFileForUploadUseCase,
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) : AbstractStartTransfersWithWorkerUseCase(cancelCancelTokenUseCase) {

    /**
     * Invoke
     */
    operator fun invoke(
        urisWithNames: Map<String, String?>,
        destinationId: NodeId,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> =
        if (monitorStorageStateEventUseCase().value.storageState == StorageState.PayWall) {
            urisWithNames.keys
                .asFlow()
                .map {
                    MultiTransferEvent.TransferNotStarted(it, StorageStatePayWallException())
                }
        } else {
            flow {
                val filesAndNames: Map<File, String?> = urisWithNames.mapKeys {
                    getFileForUploadUseCase(it.key, false)
                }.filter { it.key != null }.mapKeys { it.key!! }
                emitAll(startTransfersAndThenWorkerFlow(
                    doTransfers = {
                        uploadFilesUseCase(
                            filesAndNames = filesAndNames,
                            parentFolderId = destinationId,
                            appData = null,
                            isHighPriority = isHighPriority
                        )
                    },
                    startWorker = {
                        startUploadsWorkerAndWaitUntilIsStartedUseCase()
                    }
                ))
            }
        }
}