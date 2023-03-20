package mega.privacy.android.app.presentation.node.model.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Get folder icon drawable
 * @param folderNode [TypedFolderNode] whose icon will be returned
 * @param drawerItem [DrawerItem] where this icon will be shown.
 * This is relevant in some cases, where the icon depends on where it's shown and not only in its type, for instance outgoing have priority over camera in outgoing screen
 * @return the icon resource for this folder
 */
@DrawableRes
internal fun getFolderIcon(
    folderNode: TypedFolderNode,
    drawerItem: DrawerItem,
) = with(folderNode) {
    val outShare = isShared || isPendingShare
    val outSharePriority = outShare && drawerItem == DrawerItem.SHARED_ITEMS
    when {
        isInRubbishBin -> R.drawable.ic_folder_list
        isIncomingShare -> R.drawable.ic_folder_incoming
        outShare && outSharePriority -> R.drawable.ic_folder_outgoing
        type is FolderType.MediaSyncFolder -> R.drawable.ic_folder_camera_uploads_list
        type is FolderType.ChatFilesFolder -> R.drawable.ic_folder_chat_list
        outShare -> R.drawable.ic_folder_outgoing
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