package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import javax.inject.Inject

/**
 * Use case to check if the node is allowed to be hidden
 *
 * @property getPrimarySyncHandleUseCase           GetPrimarySyncHandleUseCase to provide the primary sync handle
 * @property getSecondarySyncHandleUseCase         GetSecondarySyncHandleUseCase to provide the secondary sync handle
 * @property getMyChatsFilesFolderIdUseCase        GetMyChatsFilesFolderIdUseCase to provide the my chats files folder id
 * @property getRootNodeUseCase                    GetRootNodeUseCase to provide the root node
 */
class IsHidingActionAllowedUseCase @Inject constructor(
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
) {

    /**
     * Check if the node is allowed to be hidden
     */
    suspend operator fun invoke(nodeId: NodeId): Boolean = nodeId.longValue !in listOf(
        getPrimarySyncHandleUseCase(),
        getSecondarySyncHandleUseCase(),
        getMyChatsFilesFolderIdUseCase()?.longValue ?: -1,
        getRootNodeUseCase()?.id?.longValue ?: 0,
    )
}
