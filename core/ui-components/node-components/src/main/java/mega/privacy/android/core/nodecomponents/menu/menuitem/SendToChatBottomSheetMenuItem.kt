package mega.privacy.android.core.nodecomponents.menu.menuitem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import timber.log.Timber
import javax.inject.Inject
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler

/**
 * Send to chat bottom sheet menu item
 *
 * @property menuAction [SendToChatMenuAction]
 * @property getNodeToAttachUseCase [GetNodeToAttachUseCase]
 * @property scope [CoroutineScope]
 */
class SendToChatBottomSheetMenuItem @Inject constructor(
    override val menuAction: SendToChatMenuAction,
    private val getNodeToAttachUseCase: GetNodeToAttachUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isConnected
            && node is TypedFileNode
            && node.isTakenDown.not()
            && isNodeInRubbish.not() && node.isNodeKeyDecrypted

    override val groupId = 7
}