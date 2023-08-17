package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import javax.inject.Inject

/**
 * Use case for getting contact verification warning flag
 */
class GetContactVerificationWarningUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository
){
    /**
     * Invoke
     */
    suspend operator fun invoke() = megaNodeRepository.getContactVerificationEnabledWarning()
}