package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use Case to Clean the Rubbish Bin in the MEGA account
 *
 */
class CleanRubbishBinUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Clean the Rubbish Bin in the MEGA account
     *
     */
    suspend operator fun invoke() = nodeRepository.cleanRubbishBin()
}
