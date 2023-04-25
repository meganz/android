package mega.privacy.android.app.presentation.transfers.model.mapper

import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

/**
 * Maps [AndroidCompletedTransferMapper] data to [AndroidCompletedTransfer] to be used in the presentation layer
 */
class AndroidCompletedTransferMapper @Inject constructor() {

    /**
     * Maps [AndroidCompletedTransferMapper] data to [AndroidCompletedTransfer]
     * @param transfer a [CompletedTransfer]
     * @return [AndroidCompletedTransfer]
     */
    operator fun invoke(transfer: CompletedTransfer) =
        AndroidCompletedTransfer(
            id = transfer.id,
            fileName = transfer.fileName,
            type = transfer.type,
            state = transfer.state,
            size = transfer.size,
            nodeHandle = transfer.nodeHandle,
            path = transfer.path,
            isOfflineFile = transfer.isOfflineFile,
            timeStamp = transfer.timeStamp,
            error = transfer.error,
            originalPath = transfer.originalPath,
            parentHandle = transfer.parentHandle,
        )
}
