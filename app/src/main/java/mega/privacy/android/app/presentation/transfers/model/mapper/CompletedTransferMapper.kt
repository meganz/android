package mega.privacy.android.app.presentation.transfers.model.mapper

import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

/**
 * Maps [AndroidCompletedTransfer] data to [CompletedTransfer] to be used in the domain layer
 */
class CompletedTransferMapper @Inject constructor() {

    /**
     * Maps [AndroidCompletedTransfer] data to [CompletedTransfer]
     * @param transfer a [AndroidCompletedTransfer]
     * @return [CompletedTransfer]
     */
    operator fun invoke(transfer: AndroidCompletedTransfer) =
        CompletedTransfer(
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
