package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.runtime.Composable
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import javax.inject.Inject

/**
 * Available offline menu item
 *
 * @param menuAction [AvailableOfflineMenuAction]
 */
class AvailableOfflineBottomSheetMenuItem @Inject constructor(
    override val menuAction: AvailableOfflineMenuAction,
    private val isFolderEmptyUseCase: IsFolderEmptyUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = !node.isAvailableOffline &&
            isNodeInRubbish.not() &&
            node.isTakenDown.not() &&
            isFolderEmptyUseCase(node).not() &&
            node.isNotS4Container()

    override val groupId = 6
}