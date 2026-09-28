package mega.privacy.android.domain.usecase.node.publiclink

import kotlinx.coroutines.CancellationException
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Get the nodes the user has created a public link for
 *
 * @property shareRepository
 * @property addNodeType
 */
class GetPublicLinksUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Invoke
     *
     * @param sortOrder the order to sort the links by
     * @return the linked nodes, skipping any that cannot be typed
     */
    suspend operator fun invoke(sortOrder: SortOrder): List<TypedNode> =
        shareRepository.getPublicLinks(sortOrder)
            .mapNotNull { node ->
                runCatching { addNodeType(node) }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
            }
}
