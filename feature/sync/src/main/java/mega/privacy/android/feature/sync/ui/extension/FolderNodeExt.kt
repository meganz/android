package mega.privacy.android.feature.sync.ui.extension

import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Get the icon resource associated to the [FolderNode]
 */
fun FolderNode.getIcon(): Int = when {
    isIncomingShare -> IconPackR.drawable.ic_folder_incoming_medium_solid
    isShared || isPendingShare -> IconPackR.drawable.ic_folder_outgoing_medium_solid
    isMediaSyncFolder() -> IconPackR.drawable.ic_folder_camera_uploads_medium_solid
    isChatFilesFolder() -> IconPackR.drawable.ic_folder_chat_medium_solid
    else -> IconPackR.drawable.ic_folder_medium_solid
}

private fun FolderNode.isMediaSyncFolder(): Boolean =
    this is TypedFolderNode && type is FolderType.MediaSyncFolder

private fun FolderNode.isChatFilesFolder(): Boolean =
    this is TypedFolderNode && type is FolderType.ChatFilesFolder