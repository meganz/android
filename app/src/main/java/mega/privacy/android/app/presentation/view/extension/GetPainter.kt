package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * This method will return different type of folder icons based on their type
 * @param this@getPainter [FolderNode]
 */
@Composable
internal fun FolderNode.getPainter(): Painter =
    painterResource(id = getIcon())

@Composable
internal fun FolderNode.getIcon(): Int {
    return if (isIncomingShare) {
        CoreUiR.drawable.ic_folder_incoming
    } else if (isShared || isPendingShare) {
        CoreUiR.drawable.ic_folder_outgoing
    } else {
        CoreUiR.drawable.ic_folder_list
    }
}