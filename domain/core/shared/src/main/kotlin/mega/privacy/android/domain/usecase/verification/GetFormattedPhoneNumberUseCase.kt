package mega.privacy.android.domain.usecase.verification

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Use case to formats the specified phoneNumber to the E.164 representation.
 */
class GetFormattedPhoneNumberUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation method to format the phone number
     */
    suspend operator fun invoke(phoneNumber: String, countryCode: String): String? =
        verificationRepository.formatPhoneNumber(phoneNumber, countryCode)
}
