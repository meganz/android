package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.entity.transfer.TransferQuotaWarningTrigger
import mega.privacy.android.domain.extension.toEpochDay
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Record that the transfer quota running low warning has been shown today for the given
 * [TransferQuotaWarningTrigger], so [ShouldShowTransferQuotaWarningUseCase] does not show it again
 * until the calendar day rolls over.
 *
 * The day is persisted rather than held in memory: the process being killed must not hand the user
 * a second warning.
 *
 * @property accountRepository             [AccountRepository]
 * @property getCurrentTimeInMillisUseCase [GetCurrentTimeInMillisUseCase]
 */
class SetTransferQuotaWarningShownUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
) {
    /**
     * Invoke
     *
     * @param trigger the kind of transfer that raised the shown warning
     */
    suspend operator fun invoke(trigger: TransferQuotaWarningTrigger) {
        accountRepository.setTransferQuotaWarningShownDay(
            trigger = trigger,
            epochDay = getCurrentTimeInMillisUseCase().toEpochDay(),
        )
    }
}
