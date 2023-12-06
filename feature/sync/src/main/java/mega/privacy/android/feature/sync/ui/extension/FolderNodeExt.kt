package mega.privacy.android.feature.sync.ui.extension

import androidx.compose.runtime.Composable
import mega.privacy.android.core.R
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Get the icon for the folder node
 */
@Composable
fun FolderNode.getIcon(): Int = when {
    isIncomingShare -> R.drawable.ic_folder_incoming
    isShared || isPendingShare -> R.drawable.ic_folder_outgoing
    else -> R.drawable.ic_folder_list
}