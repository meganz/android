package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default implementation of [GetAppSubscriptionOptions]
 */
class DefaultGetAppSubscriptionOptions @Inject constructor(
    private val accountRepository: AccountRepository,
) : GetAppSubscriptionOptions {
    override suspend fun invoke() =
        accountRepository.getSubscriptionOptions().filter { plan ->
            plan.accountType !== AccountType.BUSINESS &&
                    plan.accountType !== AccountType.PRO_FLEXI &&
                    plan.accountType !== AccountType.UNKNOWN &&
                    plan.months == 1
        }
}