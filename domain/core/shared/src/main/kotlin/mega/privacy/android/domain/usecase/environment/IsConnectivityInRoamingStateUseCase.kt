package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Get user's roaming status
 */
class IsConnectivityInRoamingStateUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository,
) {

    /**
     * Invocation to get the roaming status from the repository
     */
    suspend operator fun invoke() = verificationRepository.isRoaming()
}
