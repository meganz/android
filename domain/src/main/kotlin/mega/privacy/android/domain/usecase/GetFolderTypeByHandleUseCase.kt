package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject

/**
 * Get folder type by handle use case
 */
class GetFolderTypeByHandleUseCase @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val addNodeType: AddNodeType,
) {

    /**
     * Invoke
     *
     * @param handle
     * @return FolderType if the node exists and is a folder, null otherwise
     */
    suspend operator fun invoke(handle: Long): FolderType? =
        getNodeByHandleUseCase(handle)
            ?.let { addNodeType(it) as? TypedFolderNode }
            ?.type
}