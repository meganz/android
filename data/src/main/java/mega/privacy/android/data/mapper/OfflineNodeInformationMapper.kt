package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.domain.entity.offline.InboxOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation

internal typealias OfflineNodeInformationMapper = (@JvmSuppressWildcards OfflineInformation) -> @JvmSuppressWildcards OfflineNodeInformation

internal fun toOfflineNodeInformation(offline: OfflineInformation): OfflineNodeInformation {
    return when (offline.origin) {
        OfflineInformation.INCOMING -> IncomingShareOfflineNodeInformation(
            path = offline.path,
            name = offline.name,
            incomingHandle = offline.handleIncoming,
        )
        OfflineInformation.INBOX -> InboxOfflineNodeInformation(
            path = offline.path,
            name = offline.name
        )
        else -> OtherOfflineNodeInformation(
            path = offline.path,
            name = offline.name
        )
    }
}