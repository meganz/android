package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import javax.inject.Inject

/**
 * Use case to start all pending transfers of download type
 */
class StartAllPendingDownloadsUseCase @Inject constructor(
    transferRepository: TransferRepository,
    getPendingTransfersByTypeAndStateUseCase: GetPendingTransfersByTypeAndStateUseCase,
    updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    updatePendingTransferStartedCountUseCase: UpdatePendingTransferStartedCountUseCase,
    private val getTypedNodeFromPendingTransferUseCase: GetTypedNodeFromPendingTransferUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
) : StartAllPendingTransfersUseCase(
    TransferType.DOWNLOAD,
    transferRepository,
    getPendingTransfersByTypeAndStateUseCase,
    updatePendingTransferStateUseCase,
    updatePendingTransferStartedCountUseCase
) {
    override suspend fun doTransfer(pendingTransfer: PendingTransfer): Flow<TransferEvent> {
        val node = runCatching {
            getTypedNodeFromPendingTransferUseCase(pendingTransfer)
        }.getOrElse {
            errorOnStartingPendingTransfer(pendingTransfer, it)
            return emptyFlow()
        } ?: run {
            errorOnStartingPendingTransfer(
                pendingTransfer = pendingTransfer,
                exception = NodeDoesNotExistsException()
            )
            return emptyFlow()
        }
        return downloadNodeUseCase(
            node = node,
            destinationPath = pendingTransfer.uriPath.value,
            appData = pendingTransfer.appData,
            isHighPriority = pendingTransfer.isHighPriority,
        )
    }
}