package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SubscriptionOption

/**
 * Get subscription options filtered to get subscription options available for purchase in the app, e.g. Pro I, Pro II or Pro III plans
 */
interface GetAppSubscriptionOptions {
    /**
     * Invoke
     *
     * @return [List<SubscriptionOption>]
     */
    suspend operator fun invoke(): List<SubscriptionOption>
}