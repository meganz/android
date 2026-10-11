package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFileUseCase
import javax.inject.Inject

/**
 * Use case to start all pending transfers of upload type
 */
class StartAllPendingUploadsUseCase @Inject constructor(
    transferRepository: TransferRepository,
    getPendingTransfersByTypeAndStateUseCase: GetPendingTransfersByTypeAndStateUseCase,
    updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    updatePendingTransferStartedCountUseCase: UpdatePendingTransferStartedCountUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
) : StartAllPendingTransfersUseCase(
    TransferType.GENERAL_UPLOAD,
    transferRepository,
    getPendingTransfersByTypeAndStateUseCase,
    updatePendingTransferStateUseCase,
    updatePendingTransferStartedCountUseCase
) {
    override suspend fun doTransfers(
        pendingTransfers: List<PendingTransfer>,
        scope: CoroutineScope,
    ): Map<PendingTransfer, Flow<TransferEvent>> =
        pendingTransfers.associateWith { pendingTransfer ->
            flow { emitAll(doTransfer(pendingTransfer)) }
        }

    private suspend fun doTransfer(pendingTransfer: PendingTransfer): Flow<TransferEvent> =
        uploadFileUseCase(
            uriPath = pendingTransfer.uriPath,
            fileName = pendingTransfer.fileName,
            appData = pendingTransfer.appData,
            parentFolderId = pendingTransfer.nodeIdentifier.nodeId,
            isHighPriority = pendingTransfer.isHighPriority,
            pitagTrigger = pendingTransfer.pitagTrigger,
            pitagTarget = PitagTarget.NotApplicable,
        )
}