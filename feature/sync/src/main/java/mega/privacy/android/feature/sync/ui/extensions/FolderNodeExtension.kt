package mega.privacy.android.feature.sync.ui.extensions

import mega.privacy.android.core.R as CoreUIR
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Get the icon resource associated to the [FolderNode]
 */
fun FolderNode.getIcon(): Int = when {
    isIncomingShare -> CoreUIR.drawable.ic_folder_incoming
    isShared || isPendingShare -> CoreUIR.drawable.ic_folder_outgoing
    else -> CoreUIR.drawable.ic_folder_list
}