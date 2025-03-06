package mega.privacy.android.data.mapper.transfer.active

import mega.privacy.android.data.database.entity.ActiveTransferActionGroupEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import javax.inject.Inject

internal class ActiveTransferGroupEntityMapper @Inject constructor() {
    operator fun invoke(activeTransferActionGroup: ActiveTransferActionGroup) = with(activeTransferActionGroup) {
        ActiveTransferActionGroupEntity(
            groupId = groupId,
            transferType = transferType,
            destination = destination,
            startTime = startTime,
        )
    }
}