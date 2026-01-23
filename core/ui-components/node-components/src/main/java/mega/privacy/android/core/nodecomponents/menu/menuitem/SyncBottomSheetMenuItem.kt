package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.extension.isNotS4Container
import mega.privacy.android.core.nodecomponents.menu.menuaction.SyncMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import javax.inject.Inject

class SyncBottomSheetMenuItem @Inject constructor(
    override val menuAction: SyncMenuAction,
    private val getCameraUploadsFolderHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getMediaUploadsFolderHandleUseCase: GetSecondaryFolderNodeUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ): Boolean =
        isConnected &&
                node is TypedFolderNode &&
                !node.isTakenDown &&
                !node.isSynced &&
                !node.isUserAttributeFolder() &&
                node.isNotS4Container() && node.isNodeKeyDecrypted

    private suspend fun TypedFolderNode.isUserAttributeFolder(): Boolean {
        val isCameraUploadsFolder = this.id.longValue == getCameraUploadsFolderHandleUseCase()
        val isMediaUploadsFolder =
            this.id.longValue == getMediaUploadsFolderHandleUseCase()?.id?.longValue
        val isMyChatFilesFolder = this.id.longValue == getMyChatsFilesFolderIdUseCase()?.longValue

        return isCameraUploadsFolder || isMediaUploadsFolder || isMyChatFilesFolder
    }

    override val groupId = 4
}