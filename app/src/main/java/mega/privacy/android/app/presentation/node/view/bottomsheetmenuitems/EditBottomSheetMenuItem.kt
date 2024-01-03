package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.node.model.menuaction.EditMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.AccessPermission.FULL
import mega.privacy.android.domain.entity.shares.AccessPermission.OWNER
import mega.privacy.android.domain.entity.shares.AccessPermission.READWRITE
import javax.inject.Inject

/**
 * Edit bottom sheet menu action
 *
 * @param menuAction [EditMenuAction]
 */
class EditBottomSheetMenuItem @Inject constructor(
    override val menuAction: EditMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = !isNodeInRubbish
            && isInBackups.not()
            && node.isTakenDown.not()
            && node is FileNode
            && MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)
            && accessPermission in listOf(OWNER, READWRITE, FULL)

    override val groupId = 1
}