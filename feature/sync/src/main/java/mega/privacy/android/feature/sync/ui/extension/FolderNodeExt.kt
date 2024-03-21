package mega.privacy.android.feature.sync.ui.extension

import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Get the icon resource associated to the [FolderNode]
 */
fun FolderNode.getIcon(): Int = when {
    isIncomingShare -> IconPackR.drawable.ic_folder_incoming_medium_solid
    isShared || isPendingShare -> IconPackR.drawable.ic_folder_outgoing_medium_solid
    else -> IconPackR.drawable.ic_folder_medium_solid
}