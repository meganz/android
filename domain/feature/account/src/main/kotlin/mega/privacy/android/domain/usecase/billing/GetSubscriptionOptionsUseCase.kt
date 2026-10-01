package mega.privacy.android.domain.usecase.billing

import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject


/**
 * Get list of subscription options
 */
class GetSubscriptionOptionsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @return [List<SubscriptionOption>] if exists.
     */
    suspend operator fun invoke(): List<SubscriptionOption> =
        accountRepository.getSubscriptionOptions()
}