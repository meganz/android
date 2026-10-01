package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for getting contact verification warning flag
 */
class GetContactVerificationWarningUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = nodeRepository.getContactVerificationEnabledWarning()
}