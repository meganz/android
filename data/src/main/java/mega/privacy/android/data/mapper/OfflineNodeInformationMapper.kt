package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation

internal typealias OfflineNodeInformationMapper = (@JvmSuppressWildcards Offline) -> @JvmSuppressWildcards OfflineNodeInformation

internal fun toOfflineNodeInformation(offline: Offline): OfflineNodeInformation {
    return when (offline.origin) {
        Offline.INCOMING -> IncomingShareOfflineNodeInformation(
            path = offline.path,
            name = offline.name,
            handle = offline.handle,
            incomingHandle = offline.handleIncoming,
            isFolder = offline.isFolder,
        )

        Offline.BACKUPS -> BackupsOfflineNodeInformation(
            path = offline.path,
            name = offline.name,
            handle = offline.handle,
            isFolder = offline.isFolder,
        )

        else -> OtherOfflineNodeInformation(
            path = offline.path,
            name = offline.name,
            handle = offline.handle,
            isFolder = offline.isFolder,
        )
    }
}