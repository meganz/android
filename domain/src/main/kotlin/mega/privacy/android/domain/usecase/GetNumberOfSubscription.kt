package mega.privacy.android.domain.usecase

/**
 * Get number of subscription
 *
 */
fun interface GetNumberOfSubscription {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(forceRefresh: Boolean): Long
}