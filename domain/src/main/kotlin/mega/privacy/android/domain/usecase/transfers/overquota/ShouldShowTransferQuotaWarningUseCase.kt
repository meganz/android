package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.entity.transfer.TransferQuotaWarningTrigger
import mega.privacy.android.domain.extension.toEpochDay
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Decide whether the transfer quota running low warning should be shown.
 *
 * The SDK predicts the state for every download and every streaming read and never reports
 * recovery, so an account sitting in the orange zone would raise it again for each one. It is shown
 * at most once per calendar day per [TransferQuotaWarningTrigger], so twice a day in total.
 *
 * @property accountRepository             [AccountRepository]
 * @property getCurrentTimeInMillisUseCase [GetCurrentTimeInMillisUseCase]
 */
class ShouldShowTransferQuotaWarningUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
) {
    /**
     * Invoke
     *
     * @param trigger the kind of transfer that raised the warning
     * @return true when the warning should be shown
     */
    suspend operator fun invoke(trigger: TransferQuotaWarningTrigger): Boolean {
        val shownDay = accountRepository.getTransferQuotaWarningShownDay(trigger) ?: return true
        return shownDay != getCurrentTimeInMillisUseCase().toEpochDay()
    }
}
