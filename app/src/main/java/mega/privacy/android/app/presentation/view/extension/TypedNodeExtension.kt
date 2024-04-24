package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaNode

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

        is FolderNode -> folderInfo()
        else -> ""
    }
}

@Composable
internal fun ShareFolderNode?.getSharedNodeItemDescription(): String? {
    return this?.shareData?.let { shareData ->
        when (val count = shareData.count) {
            0 -> if (!shareData.isVerified) shareData.user else null
            1 -> if (shareData.isVerified) shareData.userFullName else null
            else -> pluralStringResource(
                id = R.plurals.general_num_shared_with,
                count = count,
                count
            )
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
    else -> mega.privacy.android.icon.pack.R.drawable.ic_generic_medium_solid
}

@Composable
internal fun ShareFolderNode?.getSharesIcon(): Int? =
    this?.shareData?.let { shareData ->
        if (shareData.isUnverifiedDistinctNode) {
            mega.privacy.android.core.R.drawable.ic_alert_triangle
        } else if (this.node.isIncomingShare) {
            when (shareData.access) {
                AccessPermission.FULL -> R.drawable.ic_shared_fullaccess
                AccessPermission.READWRITE -> R.drawable.ic_shared_read_write
                else -> R.drawable.ic_shared_read
            }
        } else null
    }

@Composable
internal fun TypedNode.getNodeTitle(): String = with(this) {
    val isUnverifiedShare =
        (this as? ShareFolderNode)?.shareData?.isUnverifiedDistinctNode == true
    return if (isIncomingShare && isUnverifiedShare && isNodeKeyDecrypted.not())
        stringResource(id = R.string.shared_items_verify_credentials_undecrypted_folder)
    else name
}

@Composable
internal fun TypedNode.getNodeLabel() = colorResource(
    id = MegaNodeUtil.getNodeLabelColor(this.label)
).takeIf { this.label != MegaNode.NODE_LBL_UNKNOWN }