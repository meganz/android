package mega.privacy.android.core.nodecomponents.extension

import androidx.annotation.DrawableRes
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.R
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Get icon drawable for a node based on its type and properties.
 *
 * @param fileTypeIconMapper Mapper to get the icon based on file type.
 * @param originShares Indicates if the node is shown in shared items drawer.
 * @return Drawable resource ID for the icon.
 */
fun TypedNode.getIcon(
    fileTypeIconMapper: FileTypeIconMapper,
    originShares: Boolean = false,
) = when (this) {
    is TypedFileNode -> fileTypeIconMapper(type.extension)
    is TypedFolderNode -> {
        // In SHARED_ITEMS drawer, outgoing share icon has priority over Camera uploads and Chat
        if (originShares
            && (isShared || isPendingShare)
            && !isInRubbishBin
            && !isIncomingShare
        ) {
            IconPackR.drawable.ic_folder_outgoing_medium_solid
        } else {
            getDefaultFolderIcon(this)
        }
    }

    else -> IconPackR.drawable.ic_generic_medium_solid
}

/**
 * Get default folder icon drawable
 * @param folderNode The folder node to get the icon for.
 */
@DrawableRes
fun getDefaultFolderIcon(
    folderNode: TypedFolderNode,
) = with(folderNode) {
    when {
        isInRubbishBin -> IconPackR.drawable.ic_folder_medium_solid
        isIncomingShare -> IconPackR.drawable.ic_folder_incoming_medium_solid
        type is FolderType.MediaSyncFolder -> IconPackR.drawable.ic_folder_camera_uploads_medium_solid
        type is FolderType.ChatFilesFolder -> IconPackR.drawable.ic_folder_chat_medium_solid
        isShared || isPendingShare -> IconPackR.drawable.ic_folder_outgoing_medium_solid
        type is FolderType.RootBackup -> IconPackR.drawable.ic_backup_medium_solid
        type is FolderType.DeviceBackup -> getDeviceFolderIcon((type as FolderType.DeviceBackup).deviceType)
        type is FolderType.ChildBackup -> IconPackR.drawable.ic_folder_backup_medium_solid
        type is FolderType.Sync -> IconPackR.drawable.ic_folder_sync_medium_solid
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

/**
 * Get the icon for a share folder node based on its share data and contact verification status.
 *
 * @param isContactVerificationOn Indicates if contact verification is enabled.
 * @return Drawable resource ID for the share icon, or null if not applicable.
 */
fun ShareFolderNode?.getSharesIcon(
    isContactVerificationOn: Boolean,
): Int? = this?.shareData?.let { shareData ->
    if (isContactVerificationOn && shareData.isUnverifiedDistinctNode) {
        IconPackR.drawable.ic_alert_triangle_small_thin_outline
    } else if (this.node.isIncomingShare) {
        when (shareData.access) {
            AccessPermission.FULL -> R.drawable.ic_star_medium_thin_outline
            AccessPermission.READWRITE -> R.drawable.ic_edit_medium_thin_outline
            else -> mega.android.core.ui.R.drawable.ic_eye_medium_thin_outline
        }
    } else null
}