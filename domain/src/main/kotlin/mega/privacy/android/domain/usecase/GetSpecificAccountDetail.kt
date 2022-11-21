package mega.privacy.android.domain.usecase

/**
 * Get specific account detail
 *
 */
fun interface GetSpecificAccountDetail {
    /**
     * Invoke
     *
     * @param storage  If true, account storage details are requested
     * @param transfer If true, account transfer details are requested
     * @param pro      If true, pro level of account is requested
     */
    suspend operator fun invoke(
        storage: Boolean,
        transfer: Boolean,
        pro: Boolean
    )
}