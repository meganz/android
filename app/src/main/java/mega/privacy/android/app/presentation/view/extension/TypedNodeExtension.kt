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
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import nz.mega.sdk.MegaNode

@Composable
internal fun TypedNode.getNodeItemDescription(showPublicLinkCreationTime: Boolean) = with(this) {
    (this as? ShareFolderNode).getSharedNodeItemDescription() ?: when (this) {
        is FileNode -> {
            val timeToFormat = if (showPublicLinkCreationTime) exportedData?.publicLinkCreationTime
                ?: modificationTime
            else modificationTime

            if (timeToFormat != 0L) {
                formatFileSize(size, LocalContext.current)
                    .plus(" · ")
                    .plus(
                        formatModifiedDate(
                            java.util.Locale(
                                Locale.current.language, Locale.current.region
                            ),
                            timeToFormat
                        )
                    )
            } else {
                formatFileSize(size, LocalContext.current)
            }
        }

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
internal fun TypedNode.getNodeTitle(): String = with(this) {
    return if (isNodeKeyDecrypted.not()) {
        if (this is FileNode)
            pluralStringResource(
                id = R.plurals.cloud_drive_undecrypted_file,
                count = 1
            )
        else stringResource(id = R.string.shared_items_verify_credentials_undecrypted_folder)
    } else name
}

@Composable
internal fun TypedNode.getNodeLabel() = colorResource(
    id = MegaNodeUtil.getNodeLabelColor(this.label)
).takeIf { this.label != MegaNode.NODE_LBL_UNKNOWN }

/**
 * Check if the node is not an S4 container
 */
internal fun TypedNode.isNotS4Container(): Boolean =
    (this as? TypedFolderNode)?.isS4Container != true