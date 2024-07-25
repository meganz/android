package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferStage
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [TransferStage] mapper from SDK int values
 */
class TransferStageMapper @Inject constructor() {
    /**
     * Invoke
     */
    operator fun invoke(stage: Number): TransferStage = when (stage.toInt()) {
        MegaTransfer.STAGE_SCAN -> TransferStage.STAGE_SCANNING
        MegaTransfer.STAGE_CREATE_TREE -> TransferStage.STAGE_CREATING_TREE
        MegaTransfer.STAGE_TRANSFERRING_FILES -> TransferStage.STAGE_TRANSFERRING_FILES
        else -> TransferStage.STAGE_NONE
    }
}