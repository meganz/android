package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Get NodeContentUri By Id Use Case
 */
class GetNodeContentUriByHandleUseCase @Inject constructor(
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val addNodeType: AddNodeType,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(handle: Long) =
        getNodeByHandleUseCase(handle)?.let { node ->
            val typedNode = addNodeType(node)
            if (typedNode is TypedFileNode) {
                getFolderLinkNodeContentUriUseCase(typedNode)
            } else {
                throw IllegalStateException("node is not a file")
            }
        } ?: throw IllegalStateException("cannot find node")
}