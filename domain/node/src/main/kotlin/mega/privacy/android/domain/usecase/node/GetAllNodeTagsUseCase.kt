package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to get all node tags.
 *
 * @property nodeRepository Repository to get node tags.
 */
class GetAllNodeTagsUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Gets all node tags.
     *
     * @param searchString String to search for.
     * @return List of node tags.
     */
    suspend operator fun invoke(searchString: String) = nodeRepository.getAllNodeTags(searchString)
}