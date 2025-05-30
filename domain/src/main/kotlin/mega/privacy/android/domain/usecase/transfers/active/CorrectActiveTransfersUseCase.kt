package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.UpdatePendingTransferStateUseCase
import javax.inject.Inject

/**
 * To ensure that the local database accurately reflects the current state of transfers by retrieving and updating transfer status information from the SDK.
 * This process is necessary to rectify any misaligned states resulting from potential event loss, such as finish, cancel, or start of a transfer,
 * we need to fix it to avoid outdated counters in [ActiveTransferTotals]
 */
class CorrectActiveTransfersUseCase @Inject constructor(
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
    private val transferRepository: TransferRepository,
    private val updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke.
     * @param transferType the transfer type we want to check, or null if we want to check all of them
     */
    suspend operator fun invoke(transferType: TransferType?) {
        val activeTransfers = if (transferType == null) {
            transferRepository.getCurrentActiveTransfers()
        } else {
            transferRepository.getCurrentActiveTransfersByType(transferType)
        }
        val inProgressTransfers = getInProgressTransfersUseCase()
            .filterNot { transfer ->
                transfer.isVoiceClip()
                        || transfer.isBackgroundTransfer()
                        || transfer.isStreamingTransfer
            }

        //update transferred bytes for each transfer
        transferRepository.updateTransferredBytes(inProgressTransfers)

        //set not-in-progress active transfers as finished, this can happen if we missed a finish event from SDK
        val notInProgressNoSDCardActiveTransfersUniqueIds = activeTransfers
            .filter { activeTransfer ->
                !activeTransfer.isFinished
                        && activeTransfer.uniqueId !in inProgressTransfers.map { it.uniqueId }
            }

        if (notInProgressNoSDCardActiveTransfersUniqueIds.isNotEmpty()) {
            //Set not in progress as finished. We are not sure if they have been cancelled or failed, we check the existence of the file to set it as cancelled as best effort approach
            transferRepository.apply {
                val (fileExists, fileNotExists) = notInProgressNoSDCardActiveTransfersUniqueIds.partition {
                    fileSystemRepository.doesUriPathExist(UriPath(it.localPath))
                }
                fileExists.map { it.uniqueId }.takeIf { it.isNotEmpty() }?.let {
                    setActiveTransfersAsFinishedByUniqueId(it, cancelled = false)
                }
                fileNotExists.map { it.uniqueId }.takeIf { it.isNotEmpty() }?.let {
                    setActiveTransfersAsFinishedByUniqueId(it, cancelled = true)
                }
                removeInProgressTransfers(
                    notInProgressNoSDCardActiveTransfersUniqueIds.map { it.uniqueId }.toSet()
                )
            }
        }

        //add in-progress active transfers if they are not added, this can happen if we missed a start event from SDK
        val inProgressNotInActiveTransfers = inProgressTransfers.filterNot { transfer ->
            activeTransfers.map { it.uniqueId }.contains(transfer.uniqueId)
        }
        if (inProgressNotInActiveTransfers.isNotEmpty()) {
            transferRepository.updateInProgressTransfers(inProgressNotInActiveTransfers)
            transferRepository.insertOrUpdateActiveTransfers(inProgressNotInActiveTransfers)
        }

        val pendingTransfersWaitingSdkScanning = if (transferType == null) {
            transferRepository.getPendingTransfersByState(PendingTransferState.SdkScanning)
        } else {
            transferRepository.getPendingTransfersByTypeAndState(
                transferType,
                PendingTransferState.SdkScanning
            )
        }

        // pending transfers that are waiting the finish of the folder scanning but are not in the sdk. Set them as errors.
        val notInProgressPendingTransfersWaitingSdkScanning =
            pendingTransfersWaitingSdkScanning.filter { pendingTransfer ->
                pendingTransfer.transferUniqueId == null ||
                        inProgressTransfers.map { it.uniqueId }
                            .contains(pendingTransfer.transferUniqueId).not()
            }
        if (notInProgressPendingTransfersWaitingSdkScanning.isNotEmpty()) {
            updatePendingTransferStateUseCase(
                notInProgressPendingTransfersWaitingSdkScanning,
                PendingTransferState.ErrorStarting
            )
            notInProgressPendingTransfersWaitingSdkScanning
                .filterNot { it.isPreviewDownload() }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    transferRepository.addCompletedTransferFromFailedPendingTransfers(
                        it,
                        UnknownError()
                    )
                }
        }
    }
}