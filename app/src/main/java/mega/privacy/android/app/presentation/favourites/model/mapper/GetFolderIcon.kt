package mega.privacy.android.app.presentation.favourites.model.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Get folder icon drawable
 */
@DrawableRes
internal fun getFolderIcon(
    folderNode: TypedFolderNode,
) = with(folderNode) {
    when {
        isInRubbishBin -> R.drawable.ic_folder_list
        isIncomingShare -> R.drawable.ic_folder_incoming
        type is FolderType.MediaSyncFolder -> R.drawable.ic_folder_camera_uploads_list
        type is FolderType.ChatFilesFolder -> R.drawable.ic_folder_chat_list
        isShared || folderNode.isPendingShare -> R.drawable.ic_folder_outgoing
        type is FolderType.RootBackup -> R.drawable.backup
        type is FolderType.DeviceBackup -> getDeviceFolderIcon((type as FolderType.DeviceBackup).deviceType)
        type is FolderType.ChildBackup -> R.drawable.ic_folder_backup
        else -> R.drawable.ic_folder_list
    }
}

private fun getDeviceFolderIcon(deviceType: DeviceType) = when (deviceType) {
    DeviceType.Windows -> R.drawable.pc_win
    DeviceType.Linux -> R.drawable.pc_linux
    DeviceType.ExternalDrive -> R.drawable.ex_drive
    DeviceType.Mac -> R.drawable.pc_mac
    DeviceType.Unknown -> R.drawable.pc
}