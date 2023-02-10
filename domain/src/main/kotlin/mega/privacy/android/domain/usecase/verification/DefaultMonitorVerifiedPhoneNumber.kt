package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

/**
 * Default monitor verified phone number
 *
 * @property verificationRepository
 */
class DefaultMonitorVerifiedPhoneNumber @Inject constructor(
    private val verificationRepository: VerificationRepository,
) : MonitorVerifiedPhoneNumber {
    override fun invoke() =
        verificationRepository.monitorVerifiedPhoneNumber().distinctUntilChanged()
}