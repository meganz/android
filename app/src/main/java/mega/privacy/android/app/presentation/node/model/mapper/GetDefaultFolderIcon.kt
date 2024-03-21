package mega.privacy.android.app.presentation.node.model.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Get folder icon drawable
 */
@DrawableRes
internal fun getDefaultFolderIcon(
    folderNode: TypedFolderNode,
) = with(folderNode) {
    when {
        isInRubbishBin -> IconPackR.drawable.ic_folder_medium_solid
        isIncomingShare -> IconPackR.drawable.ic_folder_incoming_medium_solid
        type is FolderType.MediaSyncFolder -> IconPackR.drawable.ic_folder_camera_uploads_medium_solid
        type is FolderType.ChatFilesFolder -> IconPackR.drawable.ic_folder_chat_medium_solid
        isShared || folderNode.isPendingShare -> IconPackR.drawable.ic_folder_outgoing_medium_solid
        type is FolderType.RootBackup -> IconPackR.drawable.ic_backup_medium_solid
        type is FolderType.DeviceBackup -> getDeviceFolderIcon((type as FolderType.DeviceBackup).deviceType)
        type is FolderType.ChildBackup -> IconPackR.drawable.ic_folder_backup_medium_solid
        else -> IconPackR.drawable.ic_folder_medium_solid
    }
}

private fun getDeviceFolderIcon(deviceType: DeviceType) = when (deviceType) {
    DeviceType.Windows -> IconPackR.drawable.ic_pc_windows_medium_solid
    DeviceType.Linux -> IconPackR.drawable.ic_pc_linux_medium_solid
    DeviceType.ExternalDrive -> IconPackR.drawable.ic_external_drive_medium_solid
    DeviceType.Mac -> IconPackR.drawable.ic_pc_mac_medium_solid
    DeviceType.Unknown -> IconPackR.drawable.ic_pc_medium_solid
}