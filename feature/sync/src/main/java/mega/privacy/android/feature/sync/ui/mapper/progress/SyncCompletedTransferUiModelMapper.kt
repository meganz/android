package mega.privacy.android.feature.sync.ui.mapper.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.ui.model.progress.SyncCompletedTransferUiModel
import javax.inject.Inject

/**
 * Maps one completed sync transfer to [SyncCompletedTransferUiModel].
 */
internal class SyncCompletedTransferUiModelMapper @Inject constructor() {

    operator fun invoke(transfer: SyncCompletedTransfer): SyncCompletedTransferUiModel =
        with(transfer.entry) {
            SyncCompletedTransferUiModel(
                tag = tag,
                fileName = fileName,
                direction = direction,
                location = location.orEmpty(),
                sizeBytes = totalBytes,
                finishedAtMillis = transfer.finishedAtMillis,
                averageSpeed = transfer.averageSpeed,
            )
        }
}
