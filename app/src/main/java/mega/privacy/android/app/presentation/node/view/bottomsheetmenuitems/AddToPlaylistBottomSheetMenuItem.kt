package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.app.presentation.node.model.menuaction.AddToPlaylistMenuAction
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

class AddToPlaylistBottomSheetMenuItem @Inject constructor(
    override val menuAction: AddToPlaylistMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node is FileNode && node.type is VideoFileTypeInfo

    override val groupId: Int
        get() = 3
}