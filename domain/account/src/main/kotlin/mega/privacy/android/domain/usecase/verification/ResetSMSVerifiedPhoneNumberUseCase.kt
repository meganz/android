package mega.privacy.android.domain.usecase.verification

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * A use case to reset the verified phone number
 */
class ResetSMSVerifiedPhoneNumberUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation method.
     */
    suspend operator fun invoke() = verificationRepository.resetSMSVerifiedPhoneNumber()
}
