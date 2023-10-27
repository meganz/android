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
            id = offline.id,
            path = offline.path,
            name = offline.name,
            handle = offline.handle,
            incomingHandle = offline.handleIncoming,
            isFolder = offline.isFolder,
            lastModifiedTime = offline.lastModifiedTime,
            parentId = offline.parentId
        )

        Offline.BACKUPS -> BackupsOfflineNodeInformation(
            id = offline.id,
            path = offline.path,
            name = offline.name,
            handle = offline.handle,
            isFolder = offline.isFolder,
            lastModifiedTime = offline.lastModifiedTime,
            parentId = offline.parentId
        )

        else -> OtherOfflineNodeInformation(
            id = offline.id,
            path = offline.path,
            name = offline.name,
            handle = offline.handle,
            isFolder = offline.isFolder,
            lastModifiedTime = offline.lastModifiedTime,
            parentId = offline.parentId
        )
    }
}