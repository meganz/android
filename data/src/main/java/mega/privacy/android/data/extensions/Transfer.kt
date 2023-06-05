package mega.privacy.android.data.extensions

import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer

/**
 * Temporal extension for mapping [TransferType] into MegTransfer.Type.
 * This will be removed after the usage of [MegaTransfer] is completely removed from code.
 */
fun TransferType.mapTransferType(): Int =
    if (this == TransferType.TYPE_DOWNLOAD) MegaTransfer.TYPE_DOWNLOAD
    else MegaTransfer.TYPE_UPLOAD

/**
 * Temporal extension for mapping [TransferStage] into MegaTransfer.Stage
 * This will be removed after the usage of [MegaTransfer] is completely removed from code.
 */
fun TransferStage.toTransferStage(): Long = when (this) {
    TransferStage.STAGE_SCANNING -> MegaTransfer.STAGE_SCAN.toLong()
    TransferStage.STAGE_CREATING_TREE -> MegaTransfer.STAGE_CREATE_TREE.toLong()
    else -> MegaTransfer.STAGE_TRANSFERRING_FILES.toLong()
}