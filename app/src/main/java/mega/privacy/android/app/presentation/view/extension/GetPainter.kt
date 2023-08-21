package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * This method will return different type of folder icons based on their type
 */
@Composable
internal fun TypedNode.getPainter(): Painter =
    painterResource(id = getIcon())

@Composable
internal fun TypedNode.getIcon(): Int {
    return getNodeIcon(typedNode = this, originShares = false)
}