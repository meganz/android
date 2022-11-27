package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SubscriptionOption


/**
 * Get list of subscription options
 */
fun interface GetSubscriptionOptions {
    /**
     * Invoke
     *
     * @return [List<SubscriptionOption>] if exists.
     */
    suspend operator fun invoke(): List<SubscriptionOption>
}