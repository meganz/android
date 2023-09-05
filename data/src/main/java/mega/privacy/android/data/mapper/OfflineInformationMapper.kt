package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.data.model.node.OfflineInformation.Companion.FILE
import mega.privacy.android.data.model.node.OfflineInformation.Companion.FOLDER
import mega.privacy.android.domain.entity.offline.BackupsOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.IncomingShareOfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import javax.inject.Inject

internal class OfflineInformationMapper @Inject constructor() {

    internal operator fun invoke(
        offlineNode: OfflineNodeInformation,
        parentId: Int?,
    ): OfflineInformation = OfflineInformation(
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
            is IncomingShareOfflineNodeInformation -> OfflineInformation.INCOMING
            is BackupsOfflineNodeInformation -> OfflineInformation.BACKUPS
            else -> OfflineInformation.OTHER
        }

    private fun OfflineNodeInformation.getIncomingHandle() =
        if (this is IncomingShareOfflineNodeInformation) this.incomingHandle else ""
}
