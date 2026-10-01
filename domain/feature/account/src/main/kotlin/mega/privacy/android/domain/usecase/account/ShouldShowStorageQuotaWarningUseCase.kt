package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.StorageQuotaWarningTrigger
import mega.privacy.android.domain.extension.toEpochDay
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Decide whether the storage quota warning should be shown.
 *
 * [StorageState.Red] is never rate limited but belongs to the login/reload flow only — an upload
 * attempt is warned about before the transfer starts instead. [StorageState.Orange] is shown at
 * most once per calendar day per [StorageQuotaWarningTrigger], so twice a day in total.
 *
 * @property accountRepository             [AccountRepository]
 * @property getCurrentTimeInMillisUseCase [GetCurrentTimeInMillisUseCase]
 */
class ShouldShowStorageQuotaWarningUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
) {
    /**
     * Invoke
     *
     * @param storageState the current [StorageState]
     * @param trigger      the action that raised the warning
     * @return true when the warning should be shown
     */
    suspend operator fun invoke(
        storageState: StorageState,
        trigger: StorageQuotaWarningTrigger,
    ): Boolean = when (storageState) {
        StorageState.Red -> trigger == StorageQuotaWarningTrigger.LoginOrReload
        StorageState.Orange -> !hasBeenShownToday(trigger)
        else -> false
    }

    private suspend fun hasBeenShownToday(trigger: StorageQuotaWarningTrigger): Boolean {
        val shownDay = accountRepository.getStorageQuotaWarningShownDay(trigger) ?: return false
        return shownDay == getCurrentTimeInMillisUseCase().toEpochDay()
    }
}
