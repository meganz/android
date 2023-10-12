package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.Offline.Companion.FILE
import mega.privacy.android.domain.entity.Offline.Companion.FOLDER
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import javax.inject.Inject

internal class OfflineInformationMapper @Inject constructor() {

    internal operator fun invoke(
        offlineNode: OfflineNodeInformation,
        parentId: Int?,
    ): Offline = Offline(
        id = -1, // id is not known at this point will be generated when inserted to db
        handle = offlineNode.handle,
        path = offlineNode.path,
        name = offlineNode.name,
        parentId = parentId ?: -1,
        type = if (offlineNode.isFolder) FOLDER else FILE,
        origin = offlineNode.getOrigin(),
        handleIncoming = offlineNode.getIncomingHandle(),
    )

    private fun OfflineNodeInformation.getOrigin() =
        when (this) {
            is IncomingShareOfflineNodeInformation -> Offline.INCOMING
            is BackupsOfflineNodeInformation -> Offline.BACKUPS
            else -> Offline.OTHER
        }

    private fun OfflineNodeInformation.getIncomingHandle() =
        if (this is IncomingShareOfflineNodeInformation) this.incomingHandle else ""
}
