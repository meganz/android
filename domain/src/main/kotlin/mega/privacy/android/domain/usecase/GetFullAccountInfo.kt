package mega.privacy.android.domain.usecase

/**
 * Get full account info
 *
 */
fun interface GetFullAccountInfo {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}