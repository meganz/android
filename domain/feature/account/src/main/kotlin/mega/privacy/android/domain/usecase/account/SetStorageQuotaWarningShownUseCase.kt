package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.StorageQuotaWarningTrigger
import mega.privacy.android.domain.extension.toEpochDay
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Record that a storage quota warning has been shown today for the given
 * [StorageQuotaWarningTrigger], so [ShouldShowStorageQuotaWarningUseCase] does not show it again
 * until the calendar day rolls over.
 *
 * Only [StorageState.Orange] is recorded, mirroring [ShouldShowStorageQuotaWarningUseCase] so the
 * two cannot disagree about which warnings draw from an allowance.
 *
 * @property accountRepository             [AccountRepository]
 * @property getCurrentTimeInMillisUseCase [GetCurrentTimeInMillisUseCase]
 */
class SetStorageQuotaWarningShownUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
) {
    /**
     * Invoke
     *
     * @param storageState the [StorageState] the shown warning was about
     * @param trigger      the action that raised the shown warning
     */
    suspend operator fun invoke(
        storageState: StorageState,
        trigger: StorageQuotaWarningTrigger,
    ) {
        if (storageState != StorageState.Orange) return
        accountRepository.setStorageQuotaWarningShownDay(
            trigger = trigger,
            epochDay = getCurrentTimeInMillisUseCase().toEpochDay(),
        )
    }
}
