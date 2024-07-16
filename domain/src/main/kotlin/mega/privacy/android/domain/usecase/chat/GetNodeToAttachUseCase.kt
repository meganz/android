package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.chat.message.GetAttachableNodeIdUseCase
import mega.privacy.android.domain.usecase.filenode.GetOwnNodeUseCase
import javax.inject.Inject

/**
 * Use case to get node to attach
 *
 * @property getNodeByIdUseCase [GetOwnNodeUseCase]
 * @property getAttachableNodeIdUseCase [GetAttachableNodeIdUseCase]
 */
class GetNodeToAttachUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getAttachableNodeIdUseCase: GetAttachableNodeIdUseCase,
) {

    /**
     * Invoke
     *
     * @param fileNode [TypedFileNode]
     * @return [TypedNode]
     */
    suspend operator fun invoke(fileNode: TypedFileNode) =
        getNodeByIdUseCase(getAttachableNodeIdUseCase(fileNode))
}