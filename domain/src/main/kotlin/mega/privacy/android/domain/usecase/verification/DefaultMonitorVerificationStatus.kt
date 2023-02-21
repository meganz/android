package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.SmsPermission
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.entity.verification.VerificationStatus
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import javax.inject.Inject

/**
 * Default monitor verification status
 *
 * @property monitorVerifiedPhoneNumber
 * @property monitorStorageStateEvent
 * @property verificationRepository
 */
class DefaultMonitorVerificationStatus @Inject constructor(
    private val monitorVerifiedPhoneNumber: MonitorVerifiedPhoneNumber,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val verificationRepository: VerificationRepository,
) : MonitorVerificationStatus {
    override fun invoke(): Flow<VerificationStatus> {
        return combine(
            monitorStorageStateEvent()
                .map { it.storageState == StorageState.PayWall },
            monitorVerifiedPhoneNumber(),
        ) { isPaywall, verifiedPhoneNumber ->
            val permissions = verificationRepository.getSmsPermissions()
            if (verifiedPhoneNumber is VerifiedPhoneNumber.PhoneNumber) {
                Verified(
                    phoneNumber = verifiedPhoneNumber,
                    canRequestUnblockSms = canUnBlock(isPaywall, permissions),
                    canRequestOptInVerification = canVerify(isPaywall, permissions),
                )
            } else {
                UnVerified(
                    canRequestUnblockSms = canUnBlock(isPaywall, permissions),
                    canRequestOptInVerification = canVerify(isPaywall, permissions),
                )
            }
        }
    }

    private fun canUnBlock(
        isPaywall: Boolean,
        permissions: List<SmsPermission>,
    ) = !isPaywall && permissions.contains(Unblock)

    private fun canVerify(
        isPaywall: Boolean,
        permissions: List<SmsPermission>,
    ) = !isPaywall && permissions.contains(OptInVerification)
}