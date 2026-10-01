package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Use case to get the current country code
 */
class GetCurrentCountryCodeUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation method to get the current country code from the repository
     */
    suspend operator fun invoke(): String? = verificationRepository.getCurrentCountryCode()
}
