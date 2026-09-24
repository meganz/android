package mega.privacy.android.domain.usecase.verification

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * A use case to send a verification code to a given phone number
 */
class SendSMSVerificationCodeUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation method.
     *
     * @param phoneNumber The phone number that should receive the code.
     */
    suspend operator fun invoke(phoneNumber: String) =
        verificationRepository.sendSMSVerificationCode(phoneNumber = phoneNumber)
}
