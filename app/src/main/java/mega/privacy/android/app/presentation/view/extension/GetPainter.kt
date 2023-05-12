package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * This method will return different type of folder icons based on their type
 * @param this@getPainter [FolderNode]
 */
@Composable
internal fun FolderNode.getPainter(): Painter {
    return if (isIncomingShare) {
        painterResource(id = R.drawable.ic_folder_incoming)
    } else if (isShared || isPendingShare) {
        painterResource(id = R.drawable.ic_folder_outgoing)
    } else {
        painterResource(id = R.drawable.ic_folder_list)
    }
}