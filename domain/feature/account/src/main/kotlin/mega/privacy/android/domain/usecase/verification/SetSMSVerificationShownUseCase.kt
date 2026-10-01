package mega.privacy.android.domain.usecase.verification

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Use case for setting the SMS verification display state
 */
class SetSMSVerificationShownUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation method.
     *
     * @param isShown True if the status is shown, false otherwise.
     */
    suspend operator fun invoke(isShown: Boolean) {
        verificationRepository.setSMSVerificationShown(isShown)
    }
}
