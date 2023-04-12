package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get subscription options filtered to subscription options available for purchase in the app, e.g. Pro I, Pro II or Pro III plans
 */
class GetAppSubscriptionOptionsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke() =
        accountRepository.getSubscriptionOptions().filter { plan ->
            plan.accountType !== AccountType.BUSINESS &&
                    plan.accountType !== AccountType.PRO_FLEXI &&
                    plan.accountType !== AccountType.UNKNOWN &&
                    plan.months == 1
        }
}