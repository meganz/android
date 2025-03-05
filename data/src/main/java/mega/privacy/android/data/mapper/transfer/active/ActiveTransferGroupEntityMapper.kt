package mega.privacy.android.data.mapper.transfer.active

import mega.privacy.android.data.database.entity.ActiveTransferGroupEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroup
import javax.inject.Inject

internal class ActiveTransferGroupEntityMapper @Inject constructor() {
    operator fun invoke(activeTransferGroup: ActiveTransferGroup) = with(activeTransferGroup) {
        ActiveTransferGroupEntity(
            groupId = groupId,
            transferType = transferType,
            destination = destination,
            startTime = startTime,
        )
    }
}