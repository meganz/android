package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.ResetAccountDetailsTimeStampUseCase
import javax.inject.Inject

/**
 * Use Case to Clean the Rubbish Bin in the MEGA account
 *
 */
class CleanRubbishBinUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val resetAccountDetailsTimeStampUseCase: ResetAccountDetailsTimeStampUseCase
) {
    /**
     * Clean the Rubbish Bin in the MEGA account
     *
     */
    suspend operator fun invoke() {
        nodeRepository.cleanRubbishBin()
        resetAccountDetailsTimeStampUseCase()
    }
}
