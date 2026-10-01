package mega.privacy.android.domain.usecase.filelink

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.repository.FileLinkRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Use case implementation for getting the public node from link
 */
class GetPublicNodeUseCase @Inject constructor(
    private val fileLinkRepository: FileLinkRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(url: String): TypedFileNode {
        val unTypedNode = fileLinkRepository.getPublicNode(url)
        return runCatching { addNodeType(unTypedNode) as TypedFileNode }
            .getOrElse { throw PublicNodeException.GenericError() }
    }
}