package mega.privacy.android.core.nodecomponents.extension

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.DeviceType
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.R
import mega.privacy.android.icon.pack.R as IconPackR


// A temporary file to hold copies of legacy method. It will be refactored to mapper during viewmodel implementation.
@Composable
internal fun TypedNode.getNodeItemDescription(showPublicLinkCreationTime: Boolean) = with(this) {
    (this as? ShareFolderNode).getSharedNodeItemDescription() ?: when (this) {
        is FileNode -> formatFileSize(size, LocalContext.current)
            .plus(" · ")
            .plus(
                formatModifiedDate(
                    java.util.Locale(
                        Locale.current.language, Locale.current.region
                    ),
                    if (showPublicLinkCreationTime) exportedData?.publicLinkCreationTime
                        ?: modificationTime
                    else modificationTime
                )
            )

        is FolderNode -> "TODO" // TODO
        else -> ""
    }
}

@Composable
internal fun ShareFolderNode?.getSharedNodeItemDescription(): String? {
    return this?.shareData?.let { shareData ->
        when (val count = shareData.count) {
            0 -> if (!shareData.isVerified) shareData.user else null
            1 -> if (shareData.isVerified) shareData.userFullName else null
            else -> "Shared with $count" // TODO
//            else -> pluralStringResource(
//                id = sharedR.plurals.general_num_shared_with,
//                count = count,
//                count
//            )
        }
    }
}

@Composable
internal fun TypedNode.getNodeItemThumbnail(
    fileTypeIconMapper: FileTypeIconMapper,
    originShares: Boolean = false,
) = when (this) {
    is TypedFolderNode -> this.getIcon(fileTypeIconMapper, originShares)
    is TypedFileNode -> fileTypeIconMapper(this.type.extension)
    else -> R.drawable.ic_generic_medium_solid
}

@Composable
internal fun ShareFolderNode?.getSharesIcon(
    isContactVerificationOn: Boolean,
): Int? =
    this?.shareData?.let { shareData ->
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

@Composable
internal fun TypedNode.getIcon(
    fileTypeIconMapper: FileTypeIconMapper,
    originShares: Boolean = false,
) = getNodeIcon(
    typedNode = this,
    originShares = originShares,
    fileTypeIconMapper = fileTypeIconMapper
)

@DrawableRes
fun getNodeIcon(
    typedNode: TypedNode,
    originShares: Boolean,
    fileTypeIconMapper: FileTypeIconMapper,
) = when (typedNode) {
    is TypedFileNode -> fileTypeIconMapper(typedNode.type.extension)
    is TypedFolderNode -> {
        //in SHARED_ITEMS drawer, outgoing share icon has priority over Camera uploads and Chat
        if (
        // the node is shown in shared drawer
            originShares
            // is a shared node
            && (typedNode.isShared || typedNode.isPendingShare)
            // node would be a chat or camera upload icon in another drawer
            && !typedNode.isInRubbishBin
            && !typedNode.isIncomingShare
        ) {
            IconPackR.drawable.ic_folder_outgoing_medium_solid
        } else {
            getDefaultFolderIcon(typedNode) //in other cases, default icon
        }
    }

    else -> IconPackR.drawable.ic_generic_medium_solid
}

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