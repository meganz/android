package mega.privacy.android.domain.usecase.achievements

import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Use Case to check if add phone reward is possible
 */

class IsAddPhoneRewardEnabledUseCase @Inject constructor(private val verificationRepository: VerificationRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() = with(verificationRepository.getSmsPermissions()) {
        any { it is OptInVerification } && any { it is Unblock }
    }
}
