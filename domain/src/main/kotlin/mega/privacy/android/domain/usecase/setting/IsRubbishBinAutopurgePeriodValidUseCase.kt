package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import javax.inject.Inject

/**
 * Use case to check if the rubbish bin auto purge period is valid.
 */
class IsRubbishBinAutopurgePeriodValidUseCase @Inject constructor(
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
) {
    /**
     * Invoke the use case.
     */
    operator fun invoke(days: Int): Boolean {
        val accountType = getAccountTypeUseCase()
        return when {
            accountType.isPaid -> days > RB_SCHEDULER_MINIMUM_PERIOD
            else -> days in (RB_SCHEDULER_MINIMUM_PERIOD + 1)..<RB_SCHEDULER_MAXIMUM_PERIOD
        }
    }

    companion object {
        private const val RB_SCHEDULER_MINIMUM_PERIOD = 6
        private const val RB_SCHEDULER_MAXIMUM_PERIOD = 31
    }
}