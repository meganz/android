package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.GetPathForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractStartTransfersWithWorkerUseCase
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
    private val getPathForUploadUseCase: GetPathForUploadUseCase,
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
                val uploadFileInfos: List<UploadFileInfo> =
                    urisWithNames.mapNotNull { (originalUri, name) ->
                        val uriPathToUpload = runCatching {
                            getPathForUploadUseCase(UriPath(originalUri), false)?.let {
                                UriPath(it)
                            }
                        }
                        if (uriPathToUpload.getOrNull() == null) {
                            emit(
                                MultiTransferEvent.TransferNotStarted(
                                    name,
                                    uriPathToUpload.exceptionOrNull()
                                )
                            )
                        }
                        uriPathToUpload.getOrNull()?.let { uripath ->
                            val appdata = if (uripath.value == originalUri) null else {
                                listOf(TransferAppData.OriginalContentUri(originalUri))
                            }
                            UploadFileInfo(uripath, name, appdata, 0L) //TODO modified date
                        }
                    }
                if (uploadFileInfos.isNotEmpty()) {
                    emitAll(startTransfersAndThenWorkerFlow(
                        doTransfers = {
                            uploadFilesUseCase(
                                uploadFileInfos = uploadFileInfos,
                                parentFolderId = destinationId,
                                isHighPriority = isHighPriority,
                            )
                        },
                        startWorker = {
                            startUploadsWorkerAndWaitUntilIsStartedUseCase()
                        }
                    ))
                }
            }
        }
}