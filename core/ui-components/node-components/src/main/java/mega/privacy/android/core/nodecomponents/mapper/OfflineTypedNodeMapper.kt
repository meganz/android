package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.OfflineTypedFileNode
import mega.privacy.android.core.nodecomponents.model.OfflineTypedFolderNode
import mega.privacy.android.core.nodecomponents.model.OfflineTypedNode
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import javax.inject.Inject

/**
 * Mapper to convert [OfflineFileInformation] to [OfflineTypedNode]
 *
 * Used as a fallback when a cloud node has been deleted but the offline file still exists.
 */
class OfflineTypedNodeMapper @Inject constructor() {

    /**
     * Convert [OfflineFileInformation] to the appropriate [OfflineTypedNode]
     *
     * @param offlineInfo The offline file information to convert
     * @return [OfflineTypedFileNode] for files, [OfflineTypedFolderNode] for folders
     */
    operator fun invoke(offlineInfo: OfflineFileInformation): OfflineTypedNode =
        if (offlineInfo.isFolder) {
            OfflineTypedFolderNode.from(offlineInfo)
        } else {
            OfflineTypedFileNode.from(offlineInfo)
        }
}
