package mega.privacy.android.domain.usecase.verification

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * A use case to get country calling codes
 */
class GetCountryCallingCodesUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation method.
     *
     * @return List of country calling codes.
     */
    suspend operator fun invoke(): List<String> = verificationRepository.getCountryCallingCodes()
}
