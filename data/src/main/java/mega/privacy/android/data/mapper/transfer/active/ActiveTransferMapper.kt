package mega.privacy.android.data.mapper.transfer.active

import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import javax.inject.Inject


internal class ActiveTransferMapper @Inject constructor() {
    suspend operator fun invoke(activeTransferEntity: ActiveTransferEntity) =
        with(activeTransferEntity) {
            ActiveTransfer(
                tag = tag,
                transferType = transferType,
                totalBytes = totalBytes,
                transferredBytes = transferredBytes
            )
        }
}