package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.SmsPermission
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import javax.inject.Inject

/**
 * Default monitor verification status
 *
 * @property monitorVerifiedPhoneNumber
 * @property monitorStorageStateEventUseCase
 * @property verificationRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorVerificationStatusUseCase @Inject constructor(
    private val monitorVerifiedPhoneNumber: MonitorVerifiedPhoneNumber,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val verificationRepository: VerificationRepository,
    private val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
) {
    operator fun invoke() = monitorMiscLoadedUseCase()
        .filter { it }
        .flatMapLatest {
            combine(
                monitorStorageStateEventUseCase()
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