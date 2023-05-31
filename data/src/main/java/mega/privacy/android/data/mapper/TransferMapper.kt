package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [MegaTransfer] to [Transfer] mapper
 */
class TransferMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param transfer [MegaTransfer]
     */
    operator fun invoke(transfer: MegaTransfer) = Transfer(
        type = transfer.type.mapTransferType(),
        transferredBytes = transfer.transferredBytes,
        totalBytes = transfer.totalBytes,
        localPath = transfer.path,
        parentPath = transfer.parentPath,
        nodeHandle = transfer.nodeHandle,
        parentHandle = transfer.parentHandle,
        fileName = transfer.fileName,
        stage = transfer.stage.toTransferStage(),
        tag = transfer.tag,
        speed = transfer.speed,
        isForeignOverQuota = transfer.isForeignOverquota,
        isStreamingTransfer = transfer.isStreamingTransfer,
        isFinished = transfer.isFinished,
        isFolderTransfer = transfer.isFolderTransfer,
        appData = transfer.appData.orEmpty(),
        state = transfer.state.mapTransferState(),
        priority = transfer.priority,
        notificationNumber = transfer.notificationNumber,
    )

    private fun Int.mapTransferType(): TransferType = when (this) {
        MegaTransfer.TYPE_DOWNLOAD -> TransferType.TYPE_DOWNLOAD
        MegaTransfer.TYPE_UPLOAD -> TransferType.TYPE_UPLOAD
        else -> TransferType.NONE
    }

    private fun Int.mapTransferState(): TransferState = when (this) {
        MegaTransfer.STATE_NONE -> TransferState.STATE_NONE
        MegaTransfer.STATE_QUEUED -> TransferState.STATE_QUEUED
        MegaTransfer.STATE_ACTIVE -> TransferState.STATE_ACTIVE
        MegaTransfer.STATE_PAUSED -> TransferState.STATE_PAUSED
        MegaTransfer.STATE_RETRYING -> TransferState.STATE_RETRYING
        MegaTransfer.STATE_COMPLETING -> TransferState.STATE_COMPLETING
        MegaTransfer.STATE_COMPLETED -> TransferState.STATE_COMPLETED
        MegaTransfer.STATE_CANCELLED -> TransferState.STATE_CANCELLED
        MegaTransfer.STATE_FAILED -> TransferState.STATE_FAILED
        else -> TransferState.STATE_NONE
    }

    private fun Long.toTransferStage(): TransferStage = when (this) {
        MegaTransfer.STAGE_SCAN.toLong() -> TransferStage.STAGE_SCANNING
        MegaTransfer.STAGE_CREATE_TREE.toLong() -> TransferStage.STAGE_CREATING_TREE
        MegaTransfer.STAGE_TRANSFERRING_FILES.toLong() -> TransferStage.STAGE_TRANSFERRING_FILES
        else -> TransferStage.STAGE_NONE
    }
}