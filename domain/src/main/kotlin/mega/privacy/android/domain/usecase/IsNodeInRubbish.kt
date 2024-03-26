package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Use Case that returns Boolean when the node is in rubbish.
 */
@Deprecated(
    message = "This class does not adhere to use case convention",
    replaceWith = ReplaceWith(
        expression = "mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase"
    )
)
class IsNodeInRubbish @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @param handle
     * @return Boolean that determines whether node is in rubbish or not
     */
    suspend operator fun invoke(handle: Long) = nodeRepository.isNodeInRubbishBin(NodeId(handle))
}
