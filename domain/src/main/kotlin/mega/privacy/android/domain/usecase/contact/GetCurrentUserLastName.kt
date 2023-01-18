package mega.privacy.android.domain.usecase.contact

/**
 * Get current user last name
 *
 */
fun interface GetCurrentUserLastName {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): String
}