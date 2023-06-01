package mega.privacy.android.app.presentation.transfers.model.mapper

import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

/**
 * Maps [AndroidCompletedTransfer] data to [CompletedTransfer] to be used in the domain layer
 */
@Deprecated(
    message = "This mapper is still needed for legacy code",
)
class LegacyCompletedTransferMapper @Inject constructor() {

    /**
     * Maps [AndroidCompletedTransfer] data to [CompletedTransfer]
     * @param transfer a [AndroidCompletedTransfer]
     * @return [CompletedTransfer]
     */
    operator fun invoke(transfer: AndroidCompletedTransfer) =
        CompletedTransfer(
            id = transfer.id.toInt(),
            fileName = transfer.fileName.orEmpty(),
            type = transfer.type,
            state = transfer.state,
            size = transfer.size.orEmpty(),
            handle = transfer.nodeHandle.toLong(),
            path = transfer.path.orEmpty(),
            isOffline = transfer.isOfflineFile,
            timestamp = transfer.timeStamp,
            error = transfer.error,
            originalPath = transfer.originalPath.orEmpty(),
            parentHandle = transfer.parentHandle,
        )
}
