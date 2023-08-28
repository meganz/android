package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import javax.inject.Inject

/**
 * Get the rubbish bin node
 */
class GetRubbishBinFolderUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository
) {
    /**
     * Get the rubbish bin node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    suspend operator fun invoke() =
        megaNodeRepository.getRubbishBinNode()
}