package mega.privacy.android.domain.usecase.contact

/**
 * Get current user last name
 *
 */
fun interface GetCurrentUserLastName {
    /**
     * Invoke
     *
     * @param forceRefresh force to fetch latest last name or get it from cache
     * @return String
     */
    suspend operator fun invoke(forceRefresh: Boolean): String
}