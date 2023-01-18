package mega.privacy.android.domain.usecase.contact

/**
 * Get current user first name
 *
 */
fun interface GetCurrentUserFirstName {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): String
}