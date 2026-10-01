package mega.privacy.android.domain.usecase

/**
 * Get specific account detail
 *
 */
fun interface GetExtendedAccountDetail {
    /**
     * Invoke
     *
     * @param sessions
     * @param purchases
     * @param transactions
     */
    suspend operator fun invoke(
        forceRefresh: Boolean,
        sessions: Boolean,
        purchases: Boolean,
        transactions: Boolean,
    )
}