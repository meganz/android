package mega.privacy.android.domain.usecase.contact

/**
 * Get current user email
 *
 */
fun interface GetCurrentUserEmail {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): String?
}