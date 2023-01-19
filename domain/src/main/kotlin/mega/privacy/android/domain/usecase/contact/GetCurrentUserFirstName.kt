package mega.privacy.android.domain.usecase.contact

/**
 * Get current user first name
 *
 */
fun interface GetCurrentUserFirstName {
    /**
     * Invoke
     *
     * @param forceRefresh force to fetch latest first name or get it from cache
     * @return string
     */
    suspend operator fun invoke(forceRefresh: Boolean): String
}