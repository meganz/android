package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.Subscription

/**
 * Get list of subscriptions
 */
fun interface GetSubscriptions {
    /**
     * Invoke
     *
     * @return [List<Subscription>]
     */
    suspend operator fun invoke(): List<Subscription>
}