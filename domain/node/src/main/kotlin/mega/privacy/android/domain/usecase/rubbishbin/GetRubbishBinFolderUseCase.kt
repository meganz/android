package mega.privacy.android.domain.usecase.rubbishbin

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the rubbish bin node
 */
class GetRubbishBinFolderUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {
    /**
     * Get the rubbish bin node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    suspend operator fun invoke() =
        nodeRepository.getRubbishNode()
}