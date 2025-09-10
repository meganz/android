package mega.privacy.android.core.nodecomponents.components.selectionmode

import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionModeMenuItem
import javax.inject.Inject

class NodeSelectionActionUiMapper @Inject constructor() {
    operator fun invoke(item: NodeSelectionModeMenuItem): NodeSelectionAction? =
        when (item.action) {
            is CopyMenuAction -> NodeSelectionAction.Copy
            is MoveMenuAction -> NodeSelectionAction.Move
            is HideMenuAction -> NodeSelectionAction.Hide
            is TrashMenuAction -> NodeSelectionAction.RubbishBin
            is DownloadMenuAction -> NodeSelectionAction.Download
            is ManageLinkMenuAction -> NodeSelectionAction.ShareLink(2)
            else -> null
        }
}