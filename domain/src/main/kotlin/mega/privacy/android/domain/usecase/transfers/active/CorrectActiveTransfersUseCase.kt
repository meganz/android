package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersFromSdkUseCase
import mega.privacy.android.domain.usecase.transfers.pending.UpdatePendingTransferStateUseCase
import javax.inject.Inject

/**
 * To ensure that in-memory and local database transfers accurately reflects the current state of transfers, this use-case retrieves the current transfer status information from the SDK and synchs it with the local data.
 * This process is necessary to rectify any misaligned states resulting from potential event loss, such as finish, cancel, or start of a transfer, in case of app killed by the system or potential crashes, etc.
 * we need to fix it to avoid outdated counters in [ActiveTransferTotals] for notifications and [mega.privacy.android.domain.entity.transfer.InProgressTransfer] for UI.
 */
class CorrectActiveTransfersUseCase @Inject constructor(
    private val getInProgressTransfersFromSdkUseCase: GetInProgressTransfersFromSdkUseCase,
    private val transferRepository: TransferRepository,
    private val updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val updateActiveTransfersUseCase: UpdateActiveTransfersUseCase,
    private val setActiveTransfersAsFinishedUseCase: SetActiveTransfersAsFinishedUseCase,
) {
    /**
     * Invoke.
     * @param transferType the transfer type we want to check, or null if we want to check all of them
     */
    suspend operator fun invoke(transferType: TransferType?) {
        if (!(isUserLoggedInUseCase() && rootNodeExistsUseCase())) {
            return
        }
        updateActiveTransfersUseCase()
        val activeTransfers = if (transferType == null) {
            transferRepository.getActiveTransfers()
        } else {
            transferRepository.getActiveTransfersByType(transferType)
        }
        val inProgressTransfersInSdk = getInProgressTransfersFromSdkUseCase()
            .filterNot { transfer ->
                transfer.isVoiceClip()
                        || transfer.isBackgroundTransfer()
                        || transfer.isStreamingTransfer
            }

        //update transferred bytes for each transfer
        transferRepository.updateActiveTransfersBytes(inProgressTransfersInSdk)

        //set not-in-progress active transfers as finished, this can happen if we missed a finish event from SDK
        val notInProgressActiveTransfers = activeTransfers
            .filter { activeTransfer ->
                !activeTransfer.isFinished
                        && activeTransfer.uniqueId !in inProgressTransfersInSdk.map { it.uniqueId }
            }

        if (notInProgressActiveTransfers.isNotEmpty()) {
            //Set not in progress as finished. We are not sure if they have been cancelled or failed, we check the existence of the file to set it as cancelled as best effort approach
            transferRepository.apply {
                val (fileExists, fileNotExists) = notInProgressActiveTransfers
                    .filterIsInstance<Transfer>() //Completed transfers are finished by definition, doesn't need to update its finished status
                    .partition { fileSystemRepository.doesUriPathExist(UriPath(it.localPath)) }
                fileExists.takeIf { it.isNotEmpty() }?.let {
                    setActiveTransfersAsFinishedUseCase(it, cancelled = false)
                }
                fileNotExists.takeIf { it.isNotEmpty() }?.let {
                    setActiveTransfersAsFinishedUseCase(it, cancelled = true)
                }
                removeInProgressTransfers(
                    notInProgressActiveTransfers.map { it.uniqueId }.toSet()
                )
            }
        }

        // If there are still in-progress-transfers that are not in the sdk, remove them as there's nothing we can do with them
        val notInSdkInProgressTransfers =
            transferRepository.getInProgressTransfers().filterNot { inProgressTransfer ->
                inProgressTransfer.uniqueId in inProgressTransfersInSdk.map { it.uniqueId }
            }
        if (notInSdkInProgressTransfers.isNotEmpty()) {
            transferRepository
                .removeInProgressTransfers(notInSdkInProgressTransfers.map { it.uniqueId }.toSet())
        }

        //add in-progress active transfers if they are not added, this can happen if we missed a start event from SDK
        val inProgressNotInActiveTransfers = inProgressTransfersInSdk.filterNot { transfer ->
            activeTransfers.map { it.uniqueId }.contains(transfer.uniqueId)
        }
        if (inProgressNotInActiveTransfers.isNotEmpty()) {
            inProgressNotInActiveTransfers.filterNot {
                it.isStreamingTransfer
                        || it.isBackgroundTransfer()
                        || it.isFolderTransfer
                        || it.isPreviewDownload()
            }.takeUnless { it.isEmpty() }?.let { transfers ->
                transferRepository.updateInProgressTransfers(transfers)
            }
            transferRepository.putActiveTransfers(inProgressNotInActiveTransfers)
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
                        inProgressTransfersInSdk.map { it.uniqueId }
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